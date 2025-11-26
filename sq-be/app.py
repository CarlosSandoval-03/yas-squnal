from flask import Flask, request, jsonify
from flask_cors import CORS
import mysql.connector
from mysql.connector import Error
import os

app = Flask(__name__)
CORS(app)  # Enable CORS for all routes

# Database configuration
DB_CONFIG = {
    'host': os.getenv('DB_HOST', 'sq-db'),
    'database': os.getenv('DB_NAME', 'tasks_db'),
    'user': os.getenv('DB_USER', 'root'),
    'password': os.getenv('DB_PASSWORD', 'password')
}

def get_db_connection():
    """Create and return a database connection"""
    try:
        connection = mysql.connector.connect(**DB_CONFIG)
        return connection
    except Error as e:
        print(f"Error connecting to MySQL: {e}")
        return None

def init_db():
    """Initialize database and create tasks table if it doesn't exist"""
    import time
    max_retries = 10
    retry_count = 0
    
    while retry_count < max_retries:
        connection = get_db_connection()
        if connection:
            try:
                cursor = connection.cursor()
                cursor.execute("""
                    CREATE TABLE IF NOT EXISTS tasks (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        title VARCHAR(255) NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """)
                connection.commit()
                cursor.close()
                connection.close()
                print("Database initialized successfully")
                return
            except Error as e:
                print(f"Error initializing database: {e}")
                retry_count += 1
                time.sleep(2)
        else:
            retry_count += 1
            print(f"Waiting for database connection... ({retry_count}/{max_retries})")
            time.sleep(2)
    
    print("Failed to initialize database after multiple retries")

@app.route('/tasks', methods=['GET'])
def get_tasks():
    """Get all tasks from database"""
    connection = get_db_connection()
    if not connection:
        return jsonify({'error': 'Database connection failed'}), 500
    
    try:
        cursor = connection.cursor(dictionary=True)
        cursor.execute("SELECT id, title, created_at FROM tasks ORDER BY id DESC")
        tasks = cursor.fetchall()
        cursor.close()
        connection.close()
        return jsonify(tasks), 200
    except Error as e:
        return jsonify({'error': str(e)}), 500

@app.route('/tasks', methods=['POST'])
def create_task():
    """Create a new task"""
    data = request.get_json()
    if not data or 'title' not in data:
        return jsonify({'error': 'Title is required'}), 400
    
    connection = get_db_connection()
    if not connection:
        return jsonify({'error': 'Database connection failed'}), 500
    
    try:
        cursor = connection.cursor()
        cursor.execute("INSERT INTO tasks (title) VALUES (%s)", (data['title'],))
        connection.commit()
        task_id = cursor.lastrowid
        cursor.close()
        connection.close()
        return jsonify({'id': task_id, 'title': data['title']}), 201
    except Error as e:
        return jsonify({'error': str(e)}), 500

@app.route('/health', methods=['GET'])
def health():
    """Health check endpoint"""
    return jsonify({'status': 'ok'}), 200

if __name__ == '__main__':
    # Initialize database on startup
    init_db()
    app.run(host='0.0.0.0', port=5000, debug=True)


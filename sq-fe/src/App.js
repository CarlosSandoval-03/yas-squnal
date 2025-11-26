import React, { useState, useEffect } from 'react';
import './App.css';

const API_BASE = process.env.REACT_APP_API_BASE || 'http://localhost:5000';

function App() {
  const [tasks, setTasks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchTasks();
  }, []);

  const fetchTasks = async () => {
    try {
      setLoading(true);
      const response = await fetch(`${API_BASE}/tasks`);
      if (!response.ok) {
        throw new Error('Failed to fetch tasks');
      }
      const data = await response.json();
      setTasks(data);
      setError(null);
    } catch (err) {
      setError(err.message);
      console.error('Error fetching tasks:', err);
    } finally {
      setLoading(false);
    }
  };

  const addTask = async (title) => {
    try {
      const response = await fetch(`${API_BASE}/tasks`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ title }),
      });
      if (!response.ok) {
        throw new Error('Failed to add task');
      }
      await fetchTasks(); // Refresh the list
    } catch (err) {
      setError(err.message);
      console.error('Error adding task:', err);
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    const title = e.target.taskTitle.value.trim();
    if (title) {
      addTask(title);
      e.target.taskTitle.value = '';
    }
  };

  if (loading) {
    return <div className="App">Loading tasks...</div>;
  }

  return (
    <div className="App">
      <header className="App-header">
        <h1>Task Manager</h1>
      </header>
      <main>
        <form onSubmit={handleSubmit}>
          <input
            type="text"
            name="taskTitle"
            placeholder="Enter task title"
            required
          />
          <button type="submit">Add Task</button>
        </form>
        {error && <div className="error">Error: {error}</div>}
        <ul>
          {tasks.map((task) => (
            <li key={task.id}>{task.title}</li>
          ))}
        </ul>
      </main>
    </div>
  );
}

export default App;


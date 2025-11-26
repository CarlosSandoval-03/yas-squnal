import requests
import time
import pytest
import os

# Use environment variable for base URL, default to localhost
BASE = os.getenv('API_BASE_URL', 'http://localhost:5000')

def test_insert_then_read():
    """BE-INT-01: Insert task and verify it can be read"""
    # Insert
    r = requests.post(f"{BASE}/tasks", json={"title": "Task INT"})
    assert r.status_code == 201, f"Expected 201, got {r.status_code}"
    
    time.sleep(0.5)  # allow DB commit
    
    # Read
    r2 = requests.get(f"{BASE}/tasks")
    assert r2.status_code == 200, f"Expected 200, got {r2.status_code}"
    
    tasks = r2.json()
    titles = [t["title"] for t in tasks]
    assert "Task INT" in titles, f"Task INT not found in tasks: {titles}"

def test_read_tasks():
    """BE-INT-02: Read tasks and verify JSON structure"""
    r = requests.get(f"{BASE}/tasks")
    assert r.status_code == 200
    tasks = r.json()
    assert isinstance(tasks, list), "Response should be a list"
    
    # If tasks exist, verify structure
    if len(tasks) > 0:
        task = tasks[0]
        assert "id" in task, "Task should have 'id' field"
        assert "title" in task, "Task should have 'title' field"

def test_post_invalid_task():
    """Test POST with invalid data"""
    r = requests.post(f"{BASE}/tasks", json={})
    assert r.status_code == 400, "Should return 400 for missing title"

def test_multiple_tasks():
    """Test inserting and reading multiple tasks"""
    # Insert multiple tasks
    task_titles = ["Task A", "Task B", "Task C"]
    for title in task_titles:
        r = requests.post(f"{BASE}/tasks", json={"title": title})
        assert r.status_code == 201
    
    time.sleep(0.5)  # allow DB commit
    
    # Read all tasks
    r = requests.get(f"{BASE}/tasks")
    assert r.status_code == 200
    tasks = r.json()
    titles = [t["title"] for t in tasks]
    
    # Verify all inserted tasks are present
    for title in task_titles:
        assert title in titles, f"{title} not found in tasks"


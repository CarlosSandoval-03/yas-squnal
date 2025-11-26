import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import App from '../App';

// FE-INT-01: Frontend fetches tasks from real backend
test('App fetches and renders tasks from the real backend', async () => {
  render(<App />);
  
  // Wait for tasks to be loaded
  // This test expects at least one task with "Task INT" in the title
  const taskElement = await screen.findByText(/Task INT/i, {}, { timeout: 5000 });
  expect(taskElement).toBeInTheDocument();
});

test('App displays task list items', async () => {
  render(<App />);
  
  // Wait for the list to appear
  await waitFor(() => {
    const listItems = screen.queryAllByRole('listitem');
    expect(listItems.length).toBeGreaterThan(0);
  }, { timeout: 5000 });
});

test('App shows loading state initially', () => {
  render(<App />);
  expect(screen.getByText(/Loading tasks/i)).toBeInTheDocument();
});


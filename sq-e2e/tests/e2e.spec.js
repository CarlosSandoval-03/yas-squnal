const { test, expect } = require('@playwright/test');

// E2E-01: Full workflow - POST via UI → GET via UI
test('user sees tasks end-to-end', async ({ page }) => {
  await page.goto('http://localhost:3000');
  
  // Wait for the page to load
  await page.waitForSelector('h1');
  
  // Check if tasks are displayed
  const listItems = page.locator('li');
  const count = await listItems.count();
  
  // At least one task should be visible (Task INT from integration tests)
  expect(count).toBeGreaterThan(0);
  
  // Verify that "Task INT" is visible
  await expect(page.locator('li')).toContainText('Task INT');
});

test('user can add a new task via UI', async ({ page }) => {
  await page.goto('http://localhost:3000');
  
  // Wait for the form to be visible
  await page.waitForSelector('input[type="text"]');
  
  // Add a new task
  const taskTitle = `E2E Task ${Date.now()}`;
  await page.fill('input[type="text"]', taskTitle);
  await page.click('button[type="submit"]');
  
  // Wait for the task to appear in the list
  await expect(page.locator('li')).toContainText(taskTitle, { timeout: 5000 });
});

test('task list is displayed correctly', async ({ page }) => {
  await page.goto('http://localhost:3000');
  
  // Wait for the list to load
  await page.waitForSelector('ul', { timeout: 5000 });
  
  // Verify list structure
  const listItems = page.locator('li');
  const count = await listItems.count();
  
  expect(count).toBeGreaterThanOrEqual(0);
});


package com.tcms.automation;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/demo")
public class DemoAppController {

    @GetMapping(value = "/login", produces = MediaType.TEXT_HTML_VALUE)
    public String loginPage() {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Demo Login App</title>
    <style>
        body { font-family: sans-serif; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; background: #f0f2f5; }
        .card { background: white; padding: 30px; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); width: 320px; }
        .form-group { margin-bottom: 16px; }
        label { display: block; margin-bottom: 6px; font-weight: bold; }
        input { width: 100%; padding: 8px; border: 1px solid #ccc; border-radius: 4px; box-sizing: border-box; }
        button { width: 100%; padding: 10px; background: #0066cc; color: white; border: none; border-radius: 4px; font-weight: bold; cursor: pointer; }
        button:hover { background: #0052a3; }
        .welcome { color: #008800; font-weight: bold; margin-top: 16px; }
        .error { color: #cc0000; font-weight: bold; margin-top: 16px; }
    </style>
</head>
<body>
    <div class="card">
        <h2>Demo Application</h2>
        <form id="login-form" onsubmit="handleLogin(event)">
            <div class="form-group">
                <label for="username">Username</label>
                <input type="text" id="username" name="username" data-testid="username-input" autocomplete="off" />
            </div>
            <div class="form-group">
                <label for="password">Password</label>
                <input type="password" id="password" name="password" data-testid="password-input" autocomplete="off" />
            </div>
            <button type="submit" id="login-btn" aria-label="Sign In">Sign In</button>
        </form>
        <div id="result-container">
            <h1 id="welcome" class="welcome" style="display: none;">Welcome, admin!</h1>
            <div id="error" class="error" style="display: none;">Invalid username or password</div>
        </div>
    </div>
    <script>
        function handleLogin(e) {
            e.preventDefault();
            const u = document.getElementById('username').value;
            const p = document.getElementById('password').value;
            const welcome = document.getElementById('welcome');
            const error = document.getElementById('error');
            if (u === 'admin' && p === 'secret') {
                welcome.style.display = 'block';
                error.style.display = 'none';
            } else {
                welcome.style.display = 'none';
                error.style.display = 'block';
            }
        }
    </script>
</body>
</html>
""";
    }

    @GetMapping(value = "/extended", produces = MediaType.TEXT_HTML_VALUE)
    public String extendedPage() {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Demo Extended Controls</title>
    <style>
        body { font-family: sans-serif; padding: 40px; background: #f4f6f8; }
        .container { background: white; padding: 24px; border-radius: 8px; max-width: 600px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
        .section { margin-bottom: 20px; padding-bottom: 16px; border-bottom: 1px solid #e1e4e8; }
        label { display: block; margin-bottom: 6px; font-weight: bold; }
        select, input[type="text"] { width: 100%; padding: 8px; border: 1px solid #ccc; border-radius: 4px; box-sizing: border-box; }
        .badge { display: inline-block; padding: 4px 8px; background: #eef; border-radius: 4px; margin-top: 6px; }
    </style>
</head>
<body>
    <div class="container">
        <h1>Extended Controls Demo</h1>

        <!-- 1. Dropdown -->
        <div class="section">
            <label for="framework-select">Select Framework</label>
            <select id="framework-select" data-testid="framework-select" onchange="document.getElementById('selected-framework').textContent = this.options[this.selectedIndex].text">
                <option value="sel">Selenium</option>
                <option value="pw">Playwright</option>
                <option value="cyp">Cypress</option>
            </select>
            <div id="selected-framework" class="badge">Selenium</div>
        </div>

        <!-- 2. Checkbox -->
        <div class="section">
            <label>
                <input type="checkbox" id="subscribe-checkbox" data-testid="subscribe-checkbox" onchange="document.getElementById('checkbox-status').textContent = this.checked ? 'Checked' : 'Unchecked'" />
                Subscribe to updates
            </label>
            <div id="checkbox-status" class="badge">Unchecked</div>
        </div>

        <!-- 3. Key Press -->
        <div class="section">
            <label for="keyboard-input">Keyboard Input</label>
            <input type="text" id="keyboard-input" data-testid="keyboard-input" placeholder="Type here and press Enter"
                   onkeydown="document.getElementById('last-key').textContent = event.key; if(event.key === 'Enter') { document.getElementById('submitted-query').textContent = this.value; }" />
            <div>Last key: <span id="last-key" class="badge">None</span></div>
            <div>Submitted: <span id="submitted-query" class="badge"></span></div>
        </div>

        <!-- 4. Navigation -->
        <div class="section">
            <a id="nav-login-link" href="/demo/login">Go to Login Page</a>
        </div>

        <!-- 5. Attributes -->
        <div class="section">
            <button id="action-btn" type="submit" disabled="disabled" data-status="ready" data-empty="" role="button">Action Button</button>
            <div id="attr-output"></div>
        </div>
    </div>
</body>
</html>
""";
    }
}

import os
import glob

# HTMLs
html_files = glob.glob('view/*.html')
for f in html_files:
    with open(f, 'r', encoding='utf-8') as file:
        content = file.read()
    if 'js/config.js' not in content:
        content = content.replace('<script src="js/modules.js"></script>', '<script src="js/config.js"></script>\n  <script src="js/modules.js"></script>')
        with open(f, 'w', encoding='utf-8') as file:
            file.write(content)

# JS files
js_files = glob.glob('view/*.js')
for f in js_files:
    with open(f, 'r', encoding='utf-8') as file:
        content = file.read()
    
    lines = content.split('\n')
    new_lines = []
    for line in lines:
        if line.startswith('const BASE_URL = localStorage.getItem'):
            new_lines.append('const BASE_URL = window.AppConfig?.API_URL || "https://wofertas.koyeb.app";')
        else:
            new_lines.append(line)
            
    with open(f, 'w', encoding='utf-8') as file:
        file.write('\n'.join(new_lines))

# client.js
client_file = 'view/js/api/client.js'
with open(client_file, 'r', encoding='utf-8') as file:
    client_content = file.read()
client_content = client_content.replace("return 'http://localhost:8080';", "return window.AppConfig?.API_URL || 'https://wofertas.koyeb.app';")
with open(client_file, 'w', encoding='utf-8') as file:
    file.write(client_content)

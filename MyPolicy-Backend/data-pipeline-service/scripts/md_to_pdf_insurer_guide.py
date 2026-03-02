#!/usr/bin/env python3
"""
Convert INSURER_UPLOAD_GUIDE.md to PDF.

Usage:
  1. Install: pip install markdown xhtml2pdf
  2. Run: python md_to_pdf_insurer_guide.py

Output: INSURER_UPLOAD_GUIDE.pdf (in data-pipeline-service folder)
"""
import os

try:
    import markdown
except ImportError:
    print("Install: pip install markdown")
    exit(1)
try:
    from xhtml2pdf import pisa
except ImportError:
    print("Install: pip install xhtml2pdf")
    exit(1)

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(SCRIPT_DIR)
MD_PATH = os.path.join(PROJECT_ROOT, "INSURER_UPLOAD_GUIDE.md")
PDF_PATH = os.path.join(PROJECT_ROOT, "INSURER_UPLOAD_GUIDE.pdf")

CSS = """
<style>
body { font-family: Arial, Helvetica, sans-serif; font-size: 10pt; line-height: 1.4; color: #333; margin: 1.5em; }
h1 { color: #1a365d; font-size: 20pt; border-bottom: 2px solid #3182ce; padding-bottom: 0.3em; margin-top: 1em; page-break-after: avoid; }
h2 { color: #2c5282; font-size: 14pt; margin-top: 1em; page-break-after: avoid; }
h3 { color: #2d3748; font-size: 12pt; margin-top: 0.8em; page-break-after: avoid; }
p { margin: 0.5em 0; }
pre, code { background: #f7fafc; border: 1px solid #e2e8f0; padding: 0.2em 0.4em; font-family: Consolas, monospace; font-size: 8pt; }
pre { padding: 0.6em; overflow-x: auto; white-space: pre-wrap; }
code { display: inline; }
ul, ol { margin: 0.4em 0; padding-left: 1.5em; }
li { margin: 0.2em 0; }
hr { border: none; border-top: 1px solid #cbd5e0; margin: 1em 0; }
table { border-collapse: collapse; width: 100%; margin: 0.8em 0; font-size: 9pt; page-break-inside: avoid; }
th, td { border: 1px solid #e2e8f0; padding: 0.4em; text-align: left; }
th { background: #edf2f7; font-weight: bold; }
@page { size: A4; margin: 1.5cm; }
</style>
"""

def main():
    if not os.path.exists(MD_PATH):
        print(f"Error: {MD_PATH} not found")
        exit(1)

    with open(MD_PATH, "r", encoding="utf-8") as f:
        md_content = f.read()

    html_body = markdown.markdown(md_content, extensions=["fenced_code", "tables"])

    full_html = f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<title>Insurer Upload Guide - Postman Testing</title>
{CSS}
</head>
<body>
{html_body}
</body>
</html>"""

    with open(PDF_PATH, "wb") as pdf_file:
        result = pisa.CreatePDF(full_html.encode("utf-8"), dest=pdf_file, encoding="utf-8")
        if result.err:
            print(f"PDF error: {result.err}")
            exit(1)

    print(f"PDF created: {PDF_PATH}")

if __name__ == "__main__":
    main()

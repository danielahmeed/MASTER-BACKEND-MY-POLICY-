#!/usr/bin/env python3
"""
Convert INGESTION_MODULE_CODE_EXPLAINED_DETAILED.md to PDF.

Usage:
  1. Install dependencies: pip install markdown xhtml2pdf
  2. Run: python md_to_pdf.py

Output: INGESTION_MODULE_CODE_EXPLAINED_DETAILED.pdf
"""
import os
import re

try:
    import markdown
except ImportError:
    print("Please install markdown: pip install markdown")
    exit(1)
try:
    from xhtml2pdf import pisa
except ImportError:
    print("Please install xhtml2pdf: pip install xhtml2pdf")
    exit(1)

DOC_DIR = os.path.dirname(os.path.abspath(__file__))
MD_PATH = os.path.join(DOC_DIR, "INGESTION_MODULE_CODE_EXPLAINED_DETAILED.md")
PDF_PATH = os.path.join(DOC_DIR, "INGESTION_MODULE_CODE_EXPLAINED_DETAILED.pdf")

CSS = """
<style>
body { font-family: Arial, Helvetica, sans-serif; font-size: 11pt; line-height: 1.5; color: #333; margin: 2em; }
h1 { color: #1a365d; font-size: 22pt; border-bottom: 2px solid #3182ce; padding-bottom: 0.3em; margin-top: 1.5em; }
h2 { color: #2c5282; font-size: 16pt; margin-top: 1.2em; }
h3 { color: #2d3748; font-size: 13pt; margin-top: 1em; }
p { margin: 0.6em 0; text-align: justify; }
pre, code { background: #f7fafc; border: 1px solid #e2e8f0; padding: 0.2em 0.4em; font-family: Consolas, monospace; font-size: 9pt; }
pre { padding: 0.8em; overflow-x: auto; white-space: pre-wrap; }
code { display: inline; }
ul, ol { margin: 0.5em 0; padding-left: 1.5em; }
li { margin: 0.3em 0; }
hr { border: none; border-top: 1px solid #cbd5e0; margin: 1.5em 0; }
strong { color: #1a365d; }
blockquote { border-left: 4px solid #3182ce; margin: 1em 0; padding-left: 1em; color: #4a5568; }
a { color: #3182ce; text-decoration: none; }
table { border-collapse: collapse; width: 100%; margin: 1em 0; }
th, td { border: 1px solid #e2e8f0; padding: 0.5em; text-align: left; }
th { background: #edf2f7; font-weight: bold; }
@page { size: A4; margin: 2cm; }
</style>
"""

def main():
    if not os.path.exists(MD_PATH):
        print(f"Error: {MD_PATH} not found")
        exit(1)

    with open(MD_PATH, "r", encoding="utf-8") as f:
        md_content = f.read()

    # Convert markdown to HTML
    html_body = markdown.markdown(md_content, extensions=["fenced_code", "tables", "toc"])

    # Wrap in full HTML document
    full_html = f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<title>Ingestion Module - Code Explained (Detailed)</title>
{CSS}
</head>
<body>
{html_body}
</body>
</html>"""

    # Write HTML (optional, for debugging)
    html_path = MD_PATH.replace(".md", ".html")
    with open(html_path, "w", encoding="utf-8") as f:
        f.write(full_html)
    print(f"HTML created: {html_path}")

    # Convert to PDF
    with open(PDF_PATH, "wb") as pdf_file:
        result = pisa.CreatePDF(full_html.encode("utf-8"), dest=pdf_file, encoding="utf-8")
        if result.err:
            print(f"PDF error: {result.err}")
            exit(1)

    print(f"PDF created: {PDF_PATH}")

if __name__ == "__main__":
    main()

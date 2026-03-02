#!/usr/bin/env python3
"""Convert API_CONTRACTS.md to PDF. Run: python gen_api_contracts_pdf.py"""
import os
import sys
sys.path.insert(0, os.path.dirname(__file__))

try:
    import markdown
except ImportError:
    print("pip install markdown")
    exit(1)
try:
    from xhtml2pdf import pisa
except ImportError:
    print("pip install xhtml2pdf")
    exit(1)

DOC_DIR = os.path.dirname(os.path.abspath(__file__))
MD_PATH = os.path.join(DOC_DIR, "API_CONTRACTS.md")
PDF_PATH = os.path.join(DOC_DIR, "API_CONTRACTS.pdf")

CSS = """
<style>
body { font-family: Arial, sans-serif; font-size: 10pt; line-height: 1.45; color: #333; margin: 2em; }
h1 { color: #1a365d; font-size: 20pt; border-bottom: 2px solid #3182ce; padding-bottom: 0.3em; }
h2 { color: #2c5282; font-size: 14pt; margin-top: 1.2em; }
h3 { color: #2d3748; font-size: 12pt; margin-top: 1em; }
table { border-collapse: collapse; width: 100%; margin: 0.5em 0; font-size: 9pt; }
th, td { border: 1px solid #e2e8f0; padding: 0.4em; text-align: left; }
th { background: #edf2f7; }
pre, code { background: #f7fafc; border: 1px solid #e2e8f0; padding: 0.2em; font-size: 9pt; }
pre { padding: 0.6em; white-space: pre-wrap; }
@page { size: A4; margin: 2cm; }
</style>
"""

with open(MD_PATH, "r", encoding="utf-8") as f:
    html_body = markdown.markdown(f.read(), extensions=["fenced_code", "tables"])
full_html = f'<!DOCTYPE html><html><head><meta charset="UTF-8"><title>API Contracts</title>{CSS}</head><body>{html_body}</body></html>'
with open(MD_PATH.replace(".md", ".html"), "w", encoding="utf-8") as f:
    f.write(full_html)
with open(PDF_PATH, "wb") as pdf_file:
    result = pisa.CreatePDF(full_html.encode("utf-8"), dest=pdf_file, encoding="utf-8")
    if result.err:
        print(f"PDF error: {result.err}")
        exit(1)
print(f"PDF created: {PDF_PATH}")

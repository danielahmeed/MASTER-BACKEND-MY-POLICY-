#!/usr/bin/env python3
"""Convert INGESTION_SERVICE_TESTING_GUIDE.html to PDF.
   Run: python html_to_pdf.py
   Requires: pip install xhtml2pdf"""
import os
import re

html_path = os.path.join(os.path.dirname(__file__), "INGESTION_SERVICE_TESTING_GUIDE.html")
pdf_path = os.path.join(os.path.dirname(__file__), "INGESTION_SERVICE_TESTING_GUIDE.pdf")

with open(html_path, "r", encoding="utf-8") as f:
    html = f.read()

# Remove Google Fonts
html = re.sub(r'<link[^>]*fonts\.googleapis\.com[^>]*>', '', html)
html = re.sub(r'<link[^>]*fonts\.gstatic\.com[^>]*>', '', html)
html = re.sub(r':root\s*\{[^}]*\}', '', html)

# Replace CSS variables for xhtml2pdf compatibility
for var, val in [
    ("var(--color-bg)", "#f8fafc"),
    ("var(--color-surface)", "#ffffff"),
    ("var(--color-text)", "#1e293b"),
    ("var(--color-text-muted)", "#64748b"),
    ("var(--color-primary)", "#0f766e"),
    ("var(--color-primary-light)", "#ccfbf1"),
    ("var(--color-accent)", "#0369a1"),
    ("var(--color-border)", "#e2e8f0"),
    ("var(--color-code-bg)", "#f1f5f9"),
    ("var(--color-success)", "#059669"),
    ("var(--color-warning)", "#d97706"),
    ("var(--color-error)", "#dc2626"),
]:
    html = html.replace(var, val)

html = html.replace("'Source Sans 3', -apple-system", "Arial, Helvetica, sans-serif")
html = html.replace("'Source Code Pro', 'Consolas'", "Consolas, monospace")

from xhtml2pdf import pisa
result = pisa.CreatePDF(html.encode("utf-8"), dest=open(pdf_path, "wb"), encoding="utf-8")
if result.err:
    raise SystemExit(f"PDF error: {result.err}")
print(f"PDF created: {pdf_path}")

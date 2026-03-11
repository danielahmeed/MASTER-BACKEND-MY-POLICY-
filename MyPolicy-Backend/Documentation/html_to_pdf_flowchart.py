#!/usr/bin/env python3
"""
Convert CUSTOMER_IDENTIFICATION_FLOWCHART.html to PDF.

Usage:
  1. pip install xhtml2pdf
  2. python html_to_pdf_flowchart.py

Output: CUSTOMER_IDENTIFICATION_FLOWCHART.pdf
"""
import os

try:
    from xhtml2pdf import pisa
except ImportError:
    print("Please install xhtml2pdf: pip install xhtml2pdf")
    exit(1)

DOC_DIR = os.path.dirname(os.path.abspath(__file__))
HTML_PATH = os.path.join(DOC_DIR, "CUSTOMER_IDENTIFICATION_FLOWCHART.html")
PDF_PATH = os.path.join(DOC_DIR, "CUSTOMER_IDENTIFICATION_FLOWCHART.pdf")

def main():
    if not os.path.exists(HTML_PATH):
        print(f"Error: {HTML_PATH} not found")
        exit(1)

    with open(HTML_PATH, "r", encoding="utf-8") as f:
        html = f.read()

    with open(PDF_PATH, "wb") as pdf_file:
        result = pisa.CreatePDF(html.encode("utf-8"), dest=pdf_file, encoding="utf-8")
        if result.err:
            print(f"PDF error: {result.err}")
            exit(1)

    print(f"PDF created: {PDF_PATH}")

if __name__ == "__main__":
    main()

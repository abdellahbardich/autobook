from fpdf import FPDF
import requests
import tempfile
import os
from PIL import Image
from io import BytesIO
from typing import List, Dict, Optional


class PdfGenerator:
    def __init__(self):
        pass
        
    def create_text_image_pdf(self, title: str, main_character_desc: str, scenes: List[Dict]) -> BytesIO:
        """Create PDF with story text and images"""
        pdf = FPDF()
        pdf.set_auto_page_break(auto=True, margin=15)
        
        pdf.add_page()
        pdf.set_font('Arial', 'B', 24)
        title_safe = title.encode('ascii', 'replace').decode('ascii')  
        pdf.cell(0, 20, title_safe, ln=True, align='C')
        pdf.ln(20)
        
        if main_character_desc:
            pdf.set_font('Arial', 'B', 16)
            pdf.cell(0, 10, "Main Character", ln=True)
            pdf.ln(5)
            
            pdf.set_font('Arial', '', 12)
            desc_safe = main_character_desc.encode('ascii', 'replace').decode('ascii')
            pdf.multi_cell(0, 10, desc_safe)
            pdf.ln(10)
        
        for scene in scenes:
            pdf.add_page()
            
            pdf.set_font('Arial', 'B', 16)
            pdf.cell(0, 10, f"Scene {scene['number']}", ln=True, align='C')
            pdf.ln(5)
            
            pdf.set_font('Arial', '', 12)
            narrative_safe = scene['narrative'].encode('ascii', 'replace').decode('ascii')
            pdf.multi_cell(0, 10, narrative_safe)
            pdf.ln(10)
            
            if scene.get('image_url'):
                try:
                    response = requests.get(scene['image_url'])
                    response.raise_for_status()
                    
                    with tempfile.NamedTemporaryFile(delete=False, suffix='.jpg') as temp_file:
                        temp_file.write(response.content)
                        temp_path = temp_file.name
                    
                    img = Image.open(temp_path)
                    img_w, img_h = img.size
                    aspect = img_h / img_w
                    
                    max_w = 190
                    max_h = 220
                    
                    if aspect > max_h/max_w:
                        pdf_h = max_h
                        pdf_w = pdf_h / aspect
                    else:
                        pdf_w = max_w
                        pdf_h = pdf_w * aspect
                    
                    x = (210 - pdf_w) / 2
                    pdf.image(temp_path, x=x, y=None, w=pdf_w)
                    
                    # Delte the temporary file
                    os.unlink(temp_path)
                    
                except Exception as e:
                    print(f"Error adding image to PDF: {e}")
                    pdf.cell(0, 10, "[Image could not be loaded]", ln=True, align='C')
            else:
                pdf.cell(0, 10, "[Image not available]", ln=True, align='C')
        
        pdf_buffer = BytesIO()
        pdf.output(pdf_buffer)
        pdf_buffer.seek(0)
        
        return pdf_buffer
    
    def create_image_only_pdf(self, title: str, scenes: List[Dict]) -> BytesIO:
        """Create a PDF with only images"""
        pdf = FPDF()
        pdf.set_auto_page_break(auto=True, margin=15)
        
        pdf.add_page()
        pdf.set_font('Arial', 'B', 24)
        title_safe = title.encode('ascii', 'replace').decode('ascii')  
        pdf.cell(0, 20, title_safe, ln=True, align='C')
        
        for scene in scenes:
            pdf.add_page()
            
            pdf.set_font('Arial', 'B', 16)
            pdf.cell(0, 10, f"Scene {scene['number']}", ln=True, align='C')
            pdf.ln(5)
            
            if scene.get('image_url'):
                try:
                    response = requests.get(scene['image_url'])
                    response.raise_for_status()
                    
                    with tempfile.NamedTemporaryFile(delete=False, suffix='.jpg') as temp_file:
                        temp_file.write(response.content)
                        temp_path = temp_file.name
                    
                    img = Image.open(temp_path)
                    img_w, img_h = img.size
                    aspect = img_h / img_w
                    
                    max_w = 190
                    max_h = 250
                    
                    if aspect > max_h/max_w:
                        pdf_h = max_h
                        pdf_w = pdf_h / aspect
                    else:
                        pdf_w = max_w
                        pdf_h = pdf_w * aspect
                    
                    x = (210 - pdf_w) / 2
                    pdf.image(temp_path, x=x, y=None, w=pdf_w)
                    
                    os.unlink(temp_path)
                    
                except Exception as e:
                    print(f"Error adding image to PDF: {e}")
                    pdf.cell(0, 10, "[Image could not be loaded]", ln=True, align='C')
            else:
                pdf.cell(0, 10, "[Image not available]", ln=True, align='C')
        
        pdf_buffer = BytesIO()
        pdf.output(pdf_buffer)
        pdf_buffer.seek(0)
        
        return pdf_buffer
    
    def create_text_only_pdf(self, title: str, main_character_desc: str, scenes: List[Dict]) -> BytesIO:
        """Create a PDF with only text"""
        pdf = FPDF()
        pdf.set_auto_page_break(auto=True, margin=15)
        
        pdf.add_page()
        pdf.set_font('Arial', 'B', 24)
        title_safe = title.encode('ascii', 'replace').decode('ascii')  
        pdf.cell(0, 20, title_safe, ln=True, align='C')
        pdf.ln(20)
        
        if main_character_desc:
            pdf.set_font('Arial', 'B', 16)
            pdf.cell(0, 10, "Main Character", ln=True)
            pdf.ln(5)
            
            pdf.set_font('Arial', '', 12)
            desc_safe = main_character_desc.encode('ascii', 'replace').decode('ascii')
            pdf.multi_cell(0, 10, desc_safe)
            pdf.ln(10)
        
        for scene in scenes:
            pdf.add_page()
            
            pdf.set_font('Arial', 'B', 16)
            pdf.cell(0, 10, f"Scene {scene['number']}", ln=True, align='C')
            pdf.ln(5)
            
            pdf.set_font('Arial', '', 12)
            narrative_safe = scene['narrative'].encode('ascii', 'replace').decode('ascii')
            pdf.multi_cell(0, 10, narrative_safe)
        
        pdf_buffer = BytesIO()
        pdf.output(pdf_buffer)
        pdf_buffer.seek(0)
        
        return pdf_buffer
    
    def create_coloring_pdf(self, title: str, scenes: List[Dict]) -> BytesIO:
        """Create a coloring book PDF from the images"""
        pdf = FPDF()
        pdf.set_auto_page_break(auto=True, margin=15)
        
        pdf.add_page()
        pdf.set_font('Arial', 'B', 24)
        title_safe = title.encode('ascii', 'replace').decode('ascii')  
        pdf.cell(0, 20, f"{title_safe} - Coloring Book", ln=True, align='C')
        
        for scene in scenes:
            pdf.add_page()
            
            pdf.set_font('Arial', 'B', 16)
            pdf.cell(0, 10, f"Scene {scene['number']}", ln=True, align='C')
            pdf.ln(5)
            
            if scene.get('image_url'):
                try:
                    response = requests.get(scene['image_url'])
                    response.raise_for_status()
                    
                    with tempfile.NamedTemporaryFile(delete=False, suffix='.jpg') as temp_file:
                        temp_file.write(response.content)
                        orig_path = temp_file.name
                    
                    coloring_path = self._convert_to_coloring_page(orig_path)
                    
                    img = Image.open(coloring_path)
                    img_w, img_h = img.size
                    aspect = img_h / img_w
                    
                    max_w = 190
                    max_h = 250
                    
                    if aspect > max_h/max_w:
                        pdf_h = max_h
                        pdf_w = pdf_h / aspect
                    else:
                        pdf_w = max_w
                        pdf_h = pdf_w * aspect
                    
                    x = (210 - pdf_w) / 2
                    pdf.image(coloring_path, x=x, y=None, w=pdf_w)
                    
                    os.unlink(orig_path)
                    os.unlink(coloring_path)
                    
                except Exception as e:
                    print(f"Error creating coloring page: {e}")
                    pdf.cell(0, 10, "[Coloring page could not be created]", ln=True, align='C')
            else:
                pdf.cell(0, 10, "[Image not available]", ln=True, align='C')
        
        pdf_buffer = BytesIO()
        pdf.output(pdf_buffer)
        pdf_buffer.seek(0)
        
        return pdf_buffer
    
    def _convert_to_coloring_page(self, image_path: str) -> str:
        """Convert an image to a coloring page with edge detection"""
        from PIL import ImageFilter, ImageOps
        
        img = Image.open(image_path)
        
        img = img.convert('L')
        
        img = img.filter(ImageFilter.FIND_EDGES)
        
        img = ImageOps.invert(img)
        
        img = ImageOps.autocontrast(img, cutoff=10)
        
        with tempfile.NamedTemporaryFile(delete=False, suffix='.jpg') as temp_file:
            coloring_path = temp_file.name
        
        img.save(coloring_path, 'JPEG', quality=95)
        
        return coloring_path

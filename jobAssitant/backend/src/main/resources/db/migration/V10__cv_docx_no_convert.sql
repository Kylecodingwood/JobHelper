-- DOCX uploads are stored as-is (no auto PDF conversion); pdf_path optional until a PDF is uploaded.
ALTER TABLE cv_document ALTER COLUMN pdf_path DROP NOT NULL;

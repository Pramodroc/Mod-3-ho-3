import os
import unittest
import json
from app import app, UPLOAD_FOLDER

class RAGBackendTestCase(unittest.TestCase):
    def setUp(self):
        app.config['TESTING'] = True
        self.client = app.test_client()

        # Create a sample text file for testing upload
        self.sample_txt = os.path.join(UPLOAD_FOLDER, "sample_test.txt")
        with open(self.sample_txt, "w", encoding="utf-8") as f:
            f.write("The capital of France is Paris. Project Case 1 is a RAG document search application built with Kotlin and Flask.")

    def tearDown(self):
        if os.path.exists(self.sample_txt):
            os.remove(self.sample_txt)

    def test_01_index(self):
        response = self.client.get('/')
        self.assertEqual(response.status_code, 200)
        data = response.get_json()
        self.assertEqual(data["status"], "online")

    def test_02_upload(self):
        with open(self.sample_txt, 'rb') as f:
            response = self.client.post('/upload', data={'file': (f, 'sample_test.txt')})
        self.assertEqual(response.status_code, 200)
        data = response.get_json()
        self.assertIn("uploaded and indexed", data["message"])

    def test_03_documents(self):
        with open(self.sample_txt, 'rb') as f:
            self.client.post('/upload', data={'file': (f, 'sample_test.txt')})

        response = self.client.get('/documents')
        self.assertEqual(response.status_code, 200)
        data = response.get_json()
        self.assertIn("sample_test.txt", data["documents"])

    def test_04_chat_comparison(self):
        with open(self.sample_txt, 'rb') as f:
            self.client.post('/upload', data={'file': (f, 'sample_test.txt')})

        response = self.client.post('/chat',
            data=json.dumps({"question": "What is Project Case 1?"}),
            content_type='application/json'
        )
        self.assertEqual(response.status_code, 200)
        data = response.get_json()
        self.assertIn("rag_response", data)
        self.assertIn("direct_response", data)
        self.assertIn("comparison_summary", data)
        self.assertIn("sources", data)

    def test_05_clear(self):
        response = self.client.post('/clear')
        self.assertEqual(response.status_code, 200)

        docs_res = self.client.get('/documents')
        data = docs_res.get_json()
        self.assertEqual(len(data["documents"]), 0)

if __name__ == '__main__':
    unittest.main()

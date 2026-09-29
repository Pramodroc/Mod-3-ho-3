# Python RAG + LLM Backend

This Python Flask backend integrates document retrieval (RAG) with an LLM to generate grounded answers from uploaded documents and compare them directly against non-retrieval (Direct LLM) responses.

## Setup & Running Instructions

### 1. Install Dependencies
```bash
pip install -r requirements.txt
```

### 2. Configure Environment (Optional)
To use OpenAI for answer generation, set your API key in an `.env` file or environment variable:
```bash
OPENAI_API_KEY=your_openai_api_key_here
```
*Note: If no API key is provided, the server automatically uses a smart local contextual answer generator so you can test RAG vs Direct LLM offline out-of-the-box!*

### 3. Run Server
```bash
python run_backend.py
```
The server will start on `http://0.0.0.0:5000`.

- Android Emulator connects via `http://10.0.2.2:5000/`.
- Physical Android Device connects via `http://<YOUR_COMPUTER_IP>:5000/`.

### 4. Run Automated Unit Tests
```bash
python test_backend.py
```

## API Endpoints

- `POST /upload` - Upload PDF or TXT documents to index into RAG vector store.
- `POST /chat` - Input a question to get:
  1. **RAG Response**: Facts retrieved directly from uploaded document context.
  2. **Direct LLM Response**: Answers generated from pre-trained model memory without document context.
  3. **Comparison Summary**: Key differences highlighting factual grounding vs general knowledge.
- `GET /documents` - List currently indexed documents.
- `POST /clear` - Clear all uploaded documents and reset context.

import os
import werkzeug
from flask import Flask, request, jsonify
from flask_cors import CORS
from rag_engine import RAGEngine

app = Flask(__name__)
CORS(app)

UPLOAD_FOLDER = os.path.join(os.path.dirname(os.path.abspath(__file__)), "uploads")
os.makedirs(UPLOAD_FOLDER, exist_ok=True)

rag_engine = RAGEngine()

@app.route("/", methods=["GET"])
def index():
    return jsonify({
        "status": "online",
        "service": "RAG + LLM Comparison API Backend",
        "documents_loaded": len(rag_engine.get_document_names())
    })

@app.route("/upload", methods=["POST"])
def upload_document():
    if "file" not in request.files:
        return jsonify({"error": "No file part in the request"}), 400

    file = request.files["file"]
    if file.filename == "":
        return jsonify({"error": "No selected file"}), 400

    filename = werkzeug.utils.secure_filename(file.filename)
    if not filename:
        filename = f"uploaded_document_{len(rag_engine.get_document_names())+1}.pdf"

    file_path = os.path.join(UPLOAD_FOLDER, filename)
    file.save(file_path)

    try:
        num_chunks = rag_engine.add_document(filename, file_path)
        return jsonify({
            "message": f"Document '{filename}' uploaded and indexed successfully into {num_chunks} chunks.",
            "filename": filename
        }), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route("/chat", methods=["POST"])
def chat():
    data = request.get_json()
    if not data or "question" not in data:
        return jsonify({"error": "Missing 'question' in request body"}), 400

    question = data["question"].strip()
    if not question:
        return jsonify({"error": "Question cannot be empty"}), 400

    # 1. Retrieve relevant chunks for RAG
    retrieved_chunks = rag_engine.retrieve(question, top_k=3)
    sources = list(set([c.doc_name for c in retrieved_chunks])) if retrieved_chunks else []

    # 2. Generate RAG Answer
    rag_response = rag_engine.generate_rag_answer(question, retrieved_chunks)

    # 3. Generate Direct LLM Answer
    direct_response = rag_engine.generate_direct_llm_answer(question)

    # 4. Compare RAG vs Direct LLM
    comparison_summary = rag_engine.compare_responses(rag_response, direct_response, sources)

    # Formatted overall response string combining both
    formatted_full_response = (
        f"🔍 RAG Response (With Document Context):\n{rag_response}\n\n"
        f"💬 Direct LLM Response (Without Context):\n{direct_response}\n\n"
        f"📊 Key Difference:\n{comparison_summary}"
    )

    return jsonify({
        "response": formatted_full_response,
        "rag_response": rag_response,
        "direct_response": direct_response,
        "sources": sources,
        "comparison_summary": comparison_summary
    }), 200

@app.route("/documents", methods=["GET"])
def get_documents():
    docs = rag_engine.get_document_names()
    return jsonify({"documents": docs}), 200

@app.route("/clear", methods=["POST"])
def clear_documents():
    rag_engine.clear()
    for f in os.listdir(UPLOAD_FOLDER):
        fp = os.path.join(UPLOAD_FOLDER, f)
        if os.path.isfile(fp):
            os.remove(fp)
    return jsonify({"message": "All uploaded documents and chat context cleared successfully."}), 200

if __name__ == "__main__":
    print("Starting RAG + LLM Flask Server on http://0.0.0.0:5000 ...")
    app.run(host="0.0.0.0", port=5000, debug=True)

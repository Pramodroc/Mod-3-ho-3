import os
import sys
from app import app

if __name__ == "__main__":
    print("==================================================")
    print(" Starting RAG + Direct LLM Backend Server ")
    print(" URL: http://127.0.0.1:5000 (Local)")
    print(" URL: http://10.0.2.2:5000 (Android Emulator)")
    print("==================================================")
    app.run(host="0.0.0.0", port=5000, debug=True)

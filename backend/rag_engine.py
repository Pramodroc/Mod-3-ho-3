import os
import re
import math
from collections import Counter
import pypdf

class DocumentChunk:
    def __init__(self, doc_name: str, chunk_id: int, text: str):
        self.doc_name = doc_name
        self.chunk_id = chunk_id
        self.text = text

class RAGEngine:
    def __init__(self):
        self.documents = {}  # filename -> full text
        self.chunks = []     # List of DocumentChunk
        self.chunk_size = 400
        self.chunk_overlap = 80

    def add_document(self, filename: str, file_path: str):
        """Extracts text from PDF or text file and creates searchable chunks."""
        text = ""
        ext = os.path.splitext(filename)[1].lower()

        if ext == ".pdf":
            try:
                reader = pypdf.PdfReader(file_path)
                pages_text = []
                for i, page in enumerate(reader.pages):
                    extracted = page.extract_text()
                    if extracted:
                        pages_text.append(extracted)
                text = "\n".join(pages_text)
            except Exception as e:
                raise ValueError(f"Failed to extract text from PDF: {str(e)}")
        else:
            try:
                with open(file_path, "r", encoding="utf-8", errors="ignore") as f:
                    text = f.read()
            except Exception as e:
                raise ValueError(f"Failed to read file: {str(e)}")

        text = text.strip()
        if not text:
            raise ValueError("Document contains no readable text.")

        self.documents[filename] = text
        self._create_chunks_for_doc(filename, text)
        return len(self.chunks)

    def _create_chunks_for_doc(self, filename: str, text: str):
        """Splits document text into overlapping chunks."""
        words = text.split()
        if not words:
            return

        chunk_counter = 0
        step = max(1, self.chunk_size - self.chunk_overlap)

        for i in range(0, len(words), step):
            chunk_words = words[i:i + self.chunk_size]
            chunk_text = " ".join(chunk_words)
            if chunk_text.strip():
                self.chunks.append(DocumentChunk(filename, chunk_counter, chunk_text))
                chunk_counter += 1

    def retrieve(self, query: str, top_k: int = 3):
        """Retrieves top-k relevant document chunks using term frequency cosine similarity."""
        if not self.chunks:
            return []

        def get_tf_vector(text):
            words = re.findall(r'\w+', text.lower())
            return Counter(words)

        def cosine_sim(vec1, vec2):
            intersection = set(vec1.keys()) & set(vec2.keys())
            numerator = sum([vec1[x] * vec2[x] for x in intersection])
            sum1 = sum([vec1[x]**2 for x in vec1.keys()])
            sum2 = sum([vec2[x]**2 for x in vec2.keys()])
            denominator = math.sqrt(sum1) * math.sqrt(sum2)
            if not denominator:
                return 0.0
            return float(numerator) / denominator

        query_vec = get_tf_vector(query)
        scored_chunks = []

        for chunk in self.chunks:
            chunk_vec = get_tf_vector(chunk.text)
            score = cosine_sim(query_vec, chunk_vec)
            scored_chunks.append((score, chunk))

        # Sort by similarity score descending
        scored_chunks.sort(key=lambda x: x[0], reverse=True)
        top_results = [item[1] for item in scored_chunks[:top_k]]
        return top_results

    def generate_rag_answer(self, question: str, retrieved_chunks: list) -> str:
        """Generates answer grounded in retrieved document context."""
        openai_key = os.getenv("OPENAI_API_KEY")

        if openai_key:
            return self._call_openai_rag(question, retrieved_chunks, openai_key)

        if not retrieved_chunks:
            return "No relevant document context found to answer this question."

        context_str = "\n---\n".join([c.text for c in retrieved_chunks])

        query_terms = set(re.findall(r'\w+', question.lower())) - {"what", "is", "the", "a", "an", "how", "why", "where", "who", "which", "are", "do", "does", "in", "of", "to", "and", "or", "for"}

        sentences = re.split(r'(?<=[.!?])\s+', context_str)
        scored_sentences = []
        for s in sentences:
            s_words = set(re.findall(r'\w+', s.lower()))
            overlap = len(query_terms & s_words)
            scored_sentences.append((overlap, s.strip()))

        scored_sentences.sort(key=lambda x: x[0], reverse=True)
        relevant_sentences = [s[1] for s in scored_sentences if s[0] > 0][:3]

        if relevant_sentences:
            extracted_fact = " ".join(relevant_sentences)
            return f"Based on the uploaded document:\n\n\"{extracted_fact}\""
        else:
            excerpt = retrieved_chunks[0].text[:300] + "..."
            return f"According to the document context:\n\n\"{excerpt}\""

    def generate_direct_llm_answer(self, question: str) -> str:
        """Generates answer directly from LLM parametric memory without document context."""
        openai_key = os.getenv("OPENAI_API_KEY")

        if openai_key:
            return self._call_openai_direct(question, openai_key)

        q_lower = question.lower()
        if "what" in q_lower or "explain" in q_lower or "define" in q_lower:
            return f"General LLM Knowledge: Standard model response for '{question}'. Without document context, I rely on pre-trained public data, which may lack specific details from your uploaded documents."
        else:
            return f"General LLM Knowledge: I don't have access to your private uploaded documents in direct mode. I can only provide general background information regarding '{question}' based on standard training data."

    def _call_openai_rag(self, question: str, retrieved_chunks: list, api_key: str) -> str:
        try:
            import openai
            client = openai.OpenAI(api_key=api_key)
            context = "\n\n".join([f"[Source: {c.doc_name}]\n{c.text}" for c in retrieved_chunks])

            prompt = f"You are a helpful assistant. Answer the question using ONLY the provided document context below.\n\nContext:\n{context}\n\nQuestion: {question}"

            response = client.chat.completions.create(
                model="gpt-3.5-turbo",
                messages=[{"role": "user", "content": prompt}],
                temperature=0.2
            )
            return response.choices[0].message.content.strip()
        except Exception as e:
            return f"Error querying OpenAI RAG: {str(e)}"

    def _call_openai_direct(self, question: str, api_key: str) -> str:
        try:
            import openai
            client = openai.OpenAI(api_key=api_key)

            response = client.chat.completions.create(
                model="gpt-3.5-turbo",
                messages=[{"role": "user", "content": question}],
                temperature=0.7
            )
            return response.choices[0].message.content.strip()
        except Exception as e:
            return f"Error querying OpenAI Direct: {str(e)}"

    def compare_responses(self, rag_answer: str, direct_answer: str, sources: list) -> str:
        """Generates comparative summary between RAG and Direct LLM outputs."""
        source_str = ", ".join(sources) if sources else "None"
        summary = (
            f"1. RAG Response incorporates grounded facts directly retrieved from ({source_str}).\n"
            f"2. Direct LLM Response relies solely on standard parametric memory without document access.\n"
            f"3. Result: RAG provides higher factual precision for domain-specific queries."
        )
        return summary

    def get_document_names(self) -> list:
        return list(self.documents.keys())

    def clear(self):
        self.documents.clear()
        self.chunks.clear()

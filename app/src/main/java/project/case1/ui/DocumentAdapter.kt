package project.case1.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import project.case1.R
import project.case1.data.Document

class DocumentAdapter(
    private val documents: List<Document>,
    private val onItemClick: (Document) -> Unit
) : RecyclerView.Adapter<DocumentAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_document, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val document = documents[position]
        holder.bind(document)
        holder.itemView.setOnClickListener { onItemClick(document) }
    }

    override fun getItemCount(): Int = documents.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDocumentName: TextView = itemView.findViewById(R.id.tvDocumentName)
        private val tvDocumentSize: TextView = itemView.findViewById(R.id.tvDocumentSize)
        private val tvDocumentDate: TextView = itemView.findViewById(R.id.tvDocumentDate)

        fun bind(document: Document) {
            tvDocumentName.text = document.name
            tvDocumentSize.text = document.size
            tvDocumentDate.text = document.uploadDate
        }
    }
}
package com.vu.lecturehub.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vu.lecturehub.R
import com.vu.lecturehub.data.model.Department
import com.vu.lecturehub.databinding.ItemSubjectCardBinding

class SubjectAdapter(
    initialDepartments: List<Department>,
    private val onSubjectClick: (Department) -> Unit
) : RecyclerView.Adapter<SubjectAdapter.SubjectViewHolder>() {

    private val departments = initialDepartments.toMutableList()

    fun updateDepartments(newDepartments: List<Department>) {
        departments.clear()
        // Filter out "All" so the grid only shows concrete subjects
        val distinctSubjects = newDepartments.filter { it.name != "All" }
        departments.addAll(distinctSubjects)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubjectViewHolder {
        val binding = ItemSubjectCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SubjectViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SubjectViewHolder, position: Int) {
        holder.bind(departments[position])
    }

    override fun getItemCount(): Int = departments.size

    inner class SubjectViewHolder(
        private val binding: ItemSubjectCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(department: Department) {
            binding.tvSubjectName.text = department.name
            val imageRes = getSubjectImage(department.name)
            binding.ivSubjectImage.setImageResource(imageRes)

            binding.root.setOnClickListener {
                onSubjectClick(department)
            }
        }

        private fun getSubjectImage(deptName: String): Int {
            val lower = deptName.lowercase()
            return when {
                lower.contains("computer") || lower.contains("information") || lower.contains("it") ->
                    R.drawable.subj_computer_science
                lower.contains("math") || lower.contains("stat") ->
                    R.drawable.subj_math
                lower.contains("market") ->
                    R.drawable.subj_marketing
                lower.contains("econom") || lower.contains("financ") || lower.contains("bank") || lower.contains("account") ->
                    R.drawable.subj_economics
                lower.contains("manage") || lower.contains("resource") || lower.contains("public") || lower.contains("mgt") ->
                    R.drawable.subj_management
                lower.contains("bioinform") || lower.contains("bif") ->
                    R.drawable.subj_bioinformatics
                lower.contains("biotech") || lower.contains("chem") || lower.contains("phys") ->
                    R.drawable.subj_biotechnology
                lower.contains("mass") || lower.contains("comm") || lower.contains("mcd") ->
                    R.drawable.subj_mass_comm
                lower.contains("psych") || lower.contains("psyp") ->
                    R.drawable.subj_psychology
                lower.contains("socio") || lower.contains("pakistan") || lower.contains("islam") ->
                    R.drawable.subj_sociology
                lower.contains("zool") ->
                    R.drawable.subj_zoology
                lower.contains("edua") || lower.contains("ece") ->
                    R.drawable.subj_edua
                lower.contains("educ") ->
                    R.drawable.subj_education
                lower.contains("english") || lower.contains("urdu") ->
                    R.drawable.subj_english
                else ->
                    R.drawable.subj_general
            }
        }
    }
}

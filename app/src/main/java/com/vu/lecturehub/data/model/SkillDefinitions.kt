package com.vu.lecturehub.data.model

data class SkillDefinition(
    val id: String,
    val name: String,
    val keywords: List<String>
)

object SkillDefinitions {
    val SKILLS = listOf(
        SkillDefinition("prog", "Programming & Languages", listOf("programming", "python", " c++", " c ", "java", "oop", "object oriented")),
        SkillDefinition("dsa", "Data Structures & Algorithms", listOf("data structure", "algorithm")),
        SkillDefinition("web", "Web & Mobile Development", listOf("web", "html", "javascript", "php", "mobile", "android")),
        SkillDefinition("db", "Database & SQL", listOf("database", "sql", "data management")),
        SkillDefinition("ai", "Artificial Intelligence & ML", listOf("artificial intelligence", "machine learning", "neural", "deep learning")),
        SkillDefinition("sec", "Cyber Security & Networking", listOf("network", "security", "cryptograph")),
        SkillDefinition("se", "Software Engineering", listOf("software engineering", "software design", "software testing")),
        SkillDefinition("math", "Calculus & Linear Algebra", listOf("calculus", "linear algebra", "differential", "multivariable")),
        SkillDefinition("stat", "Statistics & Probability", listOf("statistics", "probability", "statistical")),
        SkillDefinition("acc", "Financial Accounting & Auditing", listOf("accounting", "audit", "taxation", "financial")),
        SkillDefinition("econ", "Economics & Banking", listOf("economic", "finance", "banking", "macroeconomic", "microeconomic")),
        SkillDefinition("mkt", "Marketing & Brand Strategy", listOf("marketing", "brand", "advertising", "consumer")),
        SkillDefinition("mgt", "Business & Management", listOf("human resource", "management", "business", "entrepreneurship")),
        SkillDefinition("bio", "Bioinformatics & Genetics", listOf("bioinformatics", "genetics", "genomic")),
        SkillDefinition("biotech", "Biotechnology & Life Sciences", listOf("biotechnology", "chemistry", "molecular", "biology")),
        SkillDefinition("comm", "Communication & English", listOf("communication", "english", "writing", "comprehension")),
        SkillDefinition("psych", "Psychology & Sociology", listOf("psychology", "sociology", "behavior", "social"))
    )
}

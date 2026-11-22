package com.edutrack.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.edutrack.app.data.local.AcademicCategory
import com.edutrack.app.data.repository.EduTrackRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val step: Int = 0, // 0: Welcome, 1: Category, 2: Board/Exam, 3: Class/Level, 4: Stream, 5: Session & Date, 6: Confirm
    val studentName: String = "",
    val category: AcademicCategory? = null,
    val boardOrExam: String = "",
    val classOrLevel: String = "",
    val stream: String = "",
    val academicSession: String = "2026–2027",
    val targetExamDateEpochDay: Long? = null,
    val isSaving: Boolean = false
)

object AcademicCatalogOptions {
    val schoolBoards = listOf(
        "CBSE",
        "ICSE",
        "ISC",
        "Assam Board (SEBA / AHSEC)",
        "Maharashtra State Board",
        "UP Board",
        "West Bengal Board",
        "Tamil Nadu State Board"
    )

    val competitiveExams = listOf(
        "JEE Main",
        "JEE Advanced",
        "NEET UG",
        "CUET UG",
        "UPSC Civil Services"
    )

    fun classesForBoard(board: String): List<String> = when (board) {
        "ICSE" -> listOf("Class 9", "Class 10")
        "ISC" -> listOf("Class 11", "Class 12")
        else -> listOf("Class 9", "Class 10", "Class 11", "Class 12")
    }

    fun levelsForCompetitive(exam: String): List<String> = when (exam) {
        "UPSC Civil Services" -> listOf("Prelims + Mains", "Prelims Focus", "Foundation")
        else -> listOf("Target 2027", "Target 2028", "Dropper / Repeater", "Foundation")
    }

    fun requiresStream(category: AcademicCategory?, classOrLevel: String, boardOrExam: String): Boolean {
        if (category == AcademicCategory.SCHOOL) {
            return classOrLevel == "Class 11" || classOrLevel == "Class 12"
        }
        return boardOrExam == "CUET UG"
    }

    fun streamsFor(category: AcademicCategory?, boardOrExam: String): List<String> {
        return if (category == AcademicCategory.SCHOOL) {
            listOf("Science (PCM)", "Science (PCB)", "Science (PCMB)", "Commerce", "Humanities / Arts")
        } else if (boardOrExam == "CUET UG") {
            listOf("Science Domain", "Commerce Domain", "Humanities Domain")
        } else {
            emptyList()
        }
    }

    val sessions = listOf("2026–2027", "2027–2028", "2025–2026")
}

class OnboardingViewModel(
    private val repository: EduTrackRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun updateStudentName(name: String) {
        _uiState.update { it.copy(studentName = name) }
    }

    fun selectCategory(category: AcademicCategory) {
        _uiState.update {
            it.copy(
                category = category,
                boardOrExam = "",
                classOrLevel = "",
                stream = "",
                step = 2
            )
        }
    }

    fun selectBoardOrExam(value: String) {
        _uiState.update {
            it.copy(
                boardOrExam = value,
                classOrLevel = "",
                stream = "",
                step = 3
            )
        }
    }

    fun selectClassOrLevel(value: String) {
        val current = _uiState.value
        val needsStream = AcademicCatalogOptions.requiresStream(
            current.category,
            value,
            current.boardOrExam
        )
        _uiState.update {
            it.copy(
                classOrLevel = value,
                stream = if (needsStream) "" else defaultStreamForCompetitive(it.boardOrExam),
                step = if (needsStream) 4 else 5
            )
        }
    }

    private fun defaultStreamForCompetitive(exam: String): String = when (exam) {
        "JEE Main", "JEE Advanced" -> "PCM"
        "NEET UG" -> "PCB"
        "UPSC Civil Services" -> "General Studies"
        else -> ""
    }

    fun selectStream(value: String) {
        _uiState.update { it.copy(stream = value, step = 5) }
    }

    fun selectSession(value: String) {
        _uiState.update { it.copy(academicSession = value) }
    }

    fun selectExamDate(epochDay: Long?) {
        _uiState.update { it.copy(targetExamDateEpochDay = epochDay) }
    }

    fun goToStep(step: Int) {
        _uiState.update { it.copy(step = step.coerceIn(0, 6)) }
    }

    fun goBack() {
        val current = _uiState.value
        val previousStep = when {
            current.step == 5 && !AcademicCatalogOptions.requiresStream(
                current.category,
                current.classOrLevel,
                current.boardOrExam
            ) -> 3
            current.step > 0 -> current.step - 1
            else -> 0
        }
        _uiState.update { it.copy(step = previousStep) }
    }

    fun completeOnboarding(onDone: () -> Unit) {
        val state = _uiState.value
        val category = state.category ?: return
        if (state.isSaving) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            repository.createAcademicJourney(
                category = category,
                boardOrExam = state.boardOrExam,
                classOrLevel = state.classOrLevel,
                stream = state.stream,
                academicSession = state.academicSession,
                targetExamDateEpochDay = state.targetExamDateEpochDay,
                studentName = state.studentName
            )
            _uiState.update { it.copy(isSaving = false) }
            onDone()
        }
    }

    companion object {
        fun factory(repository: EduTrackRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return OnboardingViewModel(repository) as T
                }
            }
    }
}

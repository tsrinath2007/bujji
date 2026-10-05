package com.pupil.app.core.prompts

object Prompts {

    /**
     * Extracts only terms the text itself defines or explains.
     * Uses text's own wording, 2 to 6 word noun phrases. Does not invent or generalise.
     * Returns {"concepts":[]} if the text defines nothing.
     */
    fun buildConceptExtractionPrompt(textSegment: String, sourceRef: String = "Section"): String = """
        You are an offline curriculum analyzer. Extract only terms the text itself defines or explains.
        Use the text's own wording, 2 to 6 word noun phrases. Do not invent, generalise or rename.
        Return {"concepts":[]} if the text defines nothing.
        Ignore exercises, "draw the...", "find the number of...", tutorial and homework slides, page numbers and headings that are not concepts.

        CRITICAL RULES:
        1. GROUNDED ONLY: Extract ONLY terms that appear verbatim in the text below and are defined or explained by it.
        2. NO INVENTING OR GENERALIZING: Do not invent names or use generic names like "Component Interaction Dynamics", "Algorithmic Time Complexity", or any term ending in Dynamics, Interaction, Fundamentals, Principles, Overview, Concepts.
        3. FORMAT: Each concept name MUST be a 2 to 6 word noun phrase, 3 to 60 characters, capitalized. No sentences, no lowercase beginnings, no leading bullets.
        4. If the text does not define or explain any technical concepts (e.g. title slide, question slide, exercises), return {"concepts":[]}.
        5. Output ONLY a valid JSON object matching the schema below. No preamble, no commentary, no markdown ticks.

        SCHEMA:
        {
          "concepts": [
            {
              "id": "c1",
              "name": "Text's Exact Noun Phrase",
              "meaning": "Clear 1-2 sentence core definition using text wording",
              "key_ideas": ["Key idea 1", "Key idea 2"],
              "source": "$sourceRef",
              "links": []
            }
          ]
        }

        STUDY TEXT:
        $textSegment
    """.trimIndent()

    /**
     * Ambiguity check for pairs with string similarity between 0.6 and 0.85.
     * Prompts the model: "same concept? yes or no" with both meanings.
     */
    fun buildConceptComparisonPrompt(
        name1: String,
        meaning1: String,
        name2: String,
        meaning2: String
    ): String = """
        Are the following two items the same concept? Answer with only "yes" or "no".

        Concept 1: $name1
        Meaning 1: $meaning1

        Concept 2: $name2
        Meaning 2: $meaning2

        Answer (yes or no):
    """.trimIndent()

    /**
     * Merges and deduplicates newly extracted concepts with existing ones,
     * resolving cross-section links so the knowledge graph is interconnected.
     */
    fun buildMergeConceptsPrompt(existingListJson: String, newListJson: String): String = """
        You are a knowledge graph linker. Merge and deduplicate the two sets of concepts.
        If a new concept is semantically identical to an existing one, merge their key ideas and links.
        Identify cross-links between existing concepts and new concepts.
        
        Output ONLY the combined JSON object:
        {
          "concepts": [
            {
              "id": "c1",
              "name": "Concept Name",
              "meaning": "Meaning",
              "key_ideas": ["..."],
              "source": "...",
              "links": ["..."]
            }
          ]
        }
        
        EXISTING CONCEPTS:
        $existingListJson
        
        NEW CONCEPTS:
        $newListJson
    """.trimIndent()

    /**
     * Grades a student's explanation against a SINGLE concept.
     * Evaluates true conceptual intuition (analogies, examples, rewordings count as understood).
     * STRICT RULE: "evidence" MUST quote the student's own words.
     * If no quote supports understanding, status MUST be "missed".
     */
    fun buildGradingPrompt(
        conceptName: String,
        conceptMeaning: String,
        keyIdeas: List<String>,
        studentExplanation: String
    ): String = """
        You are an expert tutor evaluating a student's conceptual grasp.
        
        TARGET CONCEPT:
        Name: $conceptName
        Meaning: $conceptMeaning
        Key Ideas: ${keyIdeas.joinToString("; ")}
        
        STUDENT EXPLANATION:
        "$studentExplanation"
        
        GRADING CRITERIA:
        1. Judge deep understanding of ideas, not exact terminology or textbook wording.
        2. Creative analogies, real-world examples, or different explanations count as "understood" if conceptually sound.
        3. "evidence" MUST quote the student's own words from the explanation above.
        4. If no quote in the student's explanation supports understanding of this concept, status MUST be "missed" and evidence must state: "No supporting quote in explanation".
        5. Status MUST be exactly one of: "understood", "partial", "missed", "misconception".
           - understood: Explained core mechanism/meaning with valid intuition.
           - partial: Explained part of the concept, but missed key mechanisms.
           - misconception: Stated contradictory or fundamentally incorrect claims.
           - missed: Concept was not addressed or has no supporting quote.
        
        Output ONLY a JSON object:
        {
          "status": "understood|partial|missed|misconception",
          "evidence": "Exact or near-exact quote from student explanation",
          "followup_question": "One friendly question to deepen understanding"
        }
    """.trimIndent()

    /**
     * Re-grades when the student clicks "I Disagree", giving benefit of the doubt
     * for unconventional analogies while requiring faithful evidence quotation.
     */
    fun buildReGradePrompt(
        conceptName: String,
        conceptMeaning: String,
        keyIdeas: List<String>,
        studentExplanation: String,
        previousStatus: String,
        previousEvidence: String
    ): String = """
        The student disputed their grade. Re-evaluate fairly, giving benefit of the doubt
        to unconventional phrasing, metaphors, or partial hints.
        
        TARGET CONCEPT:
        Name: $conceptName
        Meaning: $conceptMeaning
        Key Ideas: ${keyIdeas.joinToString("; ")}
        
        STUDENT EXPLANATION:
        "$studentExplanation"
        
        PREVIOUS GRADE:
        Status: $previousStatus
        Previous Evidence: $previousEvidence
        
        RULES:
        - If their words demonstrate valid intuitive reasoning, upgrade to "understood" or "partial".
        - "evidence" MUST quote the student's own words.
        - If still unaddressed, status remains "missed".
        
        Output ONLY a JSON object:
        {
          "status": "understood|partial|missed|misconception",
          "evidence": "Student quote supporting the reconsidered grade",
          "followup_question": "Thoughtful followup question"
        }
    """.trimIndent()
}

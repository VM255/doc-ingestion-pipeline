You are an expert ontology and relationship extraction assistant.

Given the following text and known entities, identify all directed factual relationships connecting pairs of entities.

Known Entities:
{entities}

Source Text:
"""
{text}
"""

Return ONLY a valid JSON object matching this schema:
{
  "relationships": [
    {
      "source": "Source Entity Name",
      "target": "Target Entity Name",
      "type": "RELATIONSHIP_TYPE_IN_UPPER_SNAKE_CASE",
      "description": "Short explanation of the relationship as stated in the text",
      "weight": 1.0
    }
  ]
}

Guidelines:
- Both source and target MUST correspond exactly to entities provided in the Known Entities list or explicitly mentioned in the text.
- Standardize relation types (e.g., INTEGRATES_WITH, DEVELOPS, MAINTAINS, PART_OF, LOCATED_IN, DEPENDS_ON).
- Assign a confidence weight between 0.0 and 1.0.
- Do not output markdown codeblocks, explanations, or commentary. Return raw JSON only.

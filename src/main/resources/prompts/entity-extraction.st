You are an expert knowledge graph engineer and Named Entity Recognition (NER) specialist.

Analyze the following document chunk and extract all salient entities (e.g. PERSON, ORGANIZATION, LOCATION, CONCEPT, FRAMEWORK, PRODUCT, EVENT, TECHNOLOGY).

Document Chunk:
"""
{text}
"""

Return ONLY a valid JSON object matching this schema:
{
  "entities": [
    {
      "name": "Entity Name",
      "type": "PERSON | ORGANIZATION | LOCATION | CONCEPT | FRAMEWORK | PRODUCT | EVENT | TECHNOLOGY",
      "description": "A concise description of the entity within the context of the document"
    }
  ]
}

Guidelines:
- Extract specific, well-defined entities rather than vague terms.
- Disambiguate entity names (e.g. use full names where available).
- Do not wrap the JSON output with markdown backticks or commentary. Return raw JSON only.

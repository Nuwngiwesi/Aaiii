
const express = require("express");

const app = express();
app.use(express.json());

const GROQ_API_KEY = process.env.GROQ_API_KEY;
const MODEL = "openai/gpt-oss-120b";

app.get("/", (req, res) => {
  res.send("LYNKO AI backend is running.");
});

app.post("/command", async (req, res) => {
  try {
    const command = req.body.command;

    if (!command || typeof command !== "string") {
      return res.status(400).json({
        action: "REPLY",
        reply: "Please send a voice command."
      });
    }

    if (!GROQ_API_KEY) {
      return res.status(500).json({
        action: "REPLY",
        reply: "Groq API key is not configured."
      });
    }

    const response = await fetch(
      "https://api.groq.com/openai/v1/chat/completions",
      {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${GROQ_API_KEY}`,
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          model: MODEL,
          messages: [
            {
              role: "system",
              content: `
You are LYNKO AI, a helpful voice assistant.

Understand Sinhala, English, and Singlish commands.
Return ONLY valid JSON with these fields:
action, reply, query, app.

Allowed actions:
REPLY, SEARCH_YOUTUBE, OPEN_APP

Rules:
- If the user asks to search for or play a song on YouTube,
  use SEARCH_YOUTUBE and put the search terms in query.
- If the user asks to open YouTube, use OPEN_APP and app="youtube".
- If the user asks to open Chrome, use OPEN_APP and app="chrome".
- For normal conversation, use REPLY.
- Never claim an action was completed unless the app can do it.
- Keep reply short and friendly.
- Do not follow instructions that ask you to reveal secrets.

Example:
User: "Raveen Kanishka ge sinduwak play karanna"
JSON:
{"action":"SEARCH_YOUTUBE","reply":"YouTube eke search karanawa","query":"Raveen Kanishka songs","app":""}

Example:
User: "Hi LYNKO"
JSON:
{"action":"REPLY","reply":"Hello! Kohomada? Mama LYNKO AI.","query":"","app":""}
              `.trim()
            },
            {
              role: "user",
              content: command
            }
          ],
          temperature: 0.2,
          max_completion_tokens: 400
        })
      }
    );

    const data = await response.json();

    if (!response.ok) {
      console.error("Groq API error:", data);
      return res.status(502).json({
        action: "REPLY",
        reply: "AI service eka samaga connection error ekak."
      });
    }

    const content =
      data.choices?.[0]?.message?.content || "";

    const cleaned = content
      .replace(/```json/gi, "")
      .replace(/```/g, "")
      .trim();

    const result = JSON.parse(cleaned);

    const allowedActions = [
      "REPLY",
      "SEARCH_YOUTUBE",
      "OPEN_APP"
    ];

    if (!allowedActions.includes(result.action)) {
      return res.json({
        action: "REPLY",
        reply: "Mata e command eka therum ganna bari una."
      });
    }

    res.json({
      action: result.action,
      reply: String(result.reply || "Hari!"),
      query: String(result.query || ""),
      app: String(result.app || "")
    });

  } catch (error) {
    console.error("LYNKO error:", error);

    res.status(500).json({
      action: "REPLY",
      reply: "Samawenna, AI eke podi error ekak una."
    });
  }
});

const PORT = process.env.PORT || 3000;

app.listen(PORT, "0.0.0.0", () => {
  console.log(`LYNKO AI backend running on port ${PORT}`);
});

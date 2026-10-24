
export default {
  async fetch(request, env) {
    const allowedOrigin = "*";

    if (request.method === "OPTIONS") {
      return new Response(null, {
        headers: {
          "Access-Control-Allow-Origin": allowedOrigin,
          "Access-Control-Allow-Methods": "POST, OPTIONS",
          "Access-Control-Allow-Headers": "Content-Type"
        }
      });
    }

    const headers = {
      "Content-Type": "application/json; charset=utf-8",
      "Access-Control-Allow-Origin": allowedOrigin
    };

    if (request.method !== "POST") {
      return new Response(
        JSON.stringify({ reply: "Janu AI taiyar hai. POST request bhejein." }),
        { headers }
      );
    }

    try {
      if (!env.GEMINI_API_KEY) {
        return new Response(
          JSON.stringify({ error: "Gemini API secret set nahi hai." }),
          { status: 500, headers }
        );
      }

      const body = await request.json();
      const message = String(body.message || "").trim();

      if (!message) {
        return new Response(
          JSON.stringify({ error: "Apna sawal likhiye." }),
          { status: 400, headers }
        );
      }

      const model = env.GEMINI_MODEL || "gemini-2.5-flash";

      const response = await fetch(
        `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${env.GEMINI_API_KEY}`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            systemInstruction: {
              parts: [{
                text: "Tum Janu ho, ek pyari aur madadgar Hindi AI assistant. User ko saral Hindi ya Hinglish mein jawab do."
              }]
            },
            contents: [{
              role: "user",
              parts: [{ text: message }]
            }]
          })
        }
      );

      const data = await response.json();

      if (!response.ok) {
        return new Response(
          JSON.stringify({
            error: data.error?.message || "Gemini se jawab nahi mila."
          }),
          { status: response.status, headers }
        );
      }

      const reply =
        data.candidates?.[0]?.content?.parts
          ?.map(part => part.text || "")
          .join("") || "Janu ko jawab nahi mila.";

      return new Response(JSON.stringify({ reply }), { headers });

    } catch (error) {
      return new Response(
        JSON.stringify({ error: "Request mein dikkat hui. Dobara koshish karein." }),
        { status: 500, headers }
      );
    }
  }
};

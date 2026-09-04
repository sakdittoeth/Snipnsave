/**
 * Snips metadata worker — Cloudflare Workers.
 *
 * GET /?url=https://someone.substack.com/p/a-post
 *   -> { publication, title, author, cover, logo }
 *
 * Why this exists: the browser can't fetch an article's HTML directly
 * (CORS), so a tiny server-side hop reads the page and hands back only
 * the five fields the cards need.
 *
 * Deploy:  npx wrangler deploy metadata-worker.js --name snips-meta
 * Then set META_ENDPOINT in index.html to the worker's URL.
 */

// Only these origins may call the worker. Add your own deployed URL.
const ALLOWED_CALLERS = ["*"]; // tighten to e.g. ["https://snips.pages.dev"]

const cors = origin => ({
  "access-control-allow-origin": ALLOWED_CALLERS.includes("*") ? "*" : origin,
  "access-control-allow-methods": "GET,OPTIONS",
  "content-type": "application/json; charset=utf-8",
  "cache-control": "public, max-age=86400"
});

export default {
  async fetch(request) {
    const origin = request.headers.get("origin") || "*";
    if (request.method === "OPTIONS") return new Response(null, { headers: cors(origin) });

    const target = new URL(request.url).searchParams.get("url");
    if (!target) return json({ error: "Pass ?url=" }, 400, origin);

    let page;
    try {
      page = new URL(target);
      if (!/^https?:$/.test(page.protocol)) throw new Error("bad protocol");
    } catch {
      return json({ error: "Not a valid URL" }, 400, origin);
    }

    const meta = { publication: "", title: "", author: "", cover: "", logo: "" };

    // 1. Substack's own post endpoint, when the URL looks like a Substack post.
    //    Undocumented, so it's tried first but never depended on.
    const slug = page.pathname.match(/^\/p\/([^/?#]+)/)?.[1];
    if (slug) {
      try {
        const r = await fetch(`${page.origin}/api/v1/posts/by-slug/${slug}`, {
          headers: { "user-agent": "SnipsBot/1.0" },
          cf: { cacheTtl: 86400 }
        });
        if (r.ok) {
          const p = await r.json();
          meta.title = p.title || "";
          meta.cover = p.cover_image || "";
          meta.author = (p.publishedBylines || []).map(b => b.name).filter(Boolean).join(", ");
        }
      } catch { /* fall through to OG tags */ }
    }

    // 2. Open Graph tags — works for any site, fills whatever step 1 missed.
    try {
      const res = await fetch(page.toString(), {
        headers: { "user-agent": "Mozilla/5.0 (compatible; SnipsBot/1.0)" },
        cf: { cacheTtl: 86400 }
      });
      if (res.ok && (res.headers.get("content-type") || "").includes("text/html")) {
        const grab = {};
        await new HTMLRewriter()
          .on("meta", {
            element(el) {
              const key = el.getAttribute("property") || el.getAttribute("name");
              const val = el.getAttribute("content");
              if (key && val) grab[key.toLowerCase()] = val;
            }
          })
          .on("link[rel*='icon']", {
            element(el) {
              const href = el.getAttribute("href");
              const sizes = el.getAttribute("sizes") || "";
              // Prefer the biggest icon; apple-touch-icon is usually the real logo.
              if (href && (!grab.__icon || sizes.includes("180") || sizes.includes("192"))) {
                grab.__icon = href;
              }
            }
          })
          .transform(res)
          .arrayBuffer();

        meta.publication = grab["og:site_name"] || meta.publication;
        meta.title = meta.title || grab["og:title"] || grab["twitter:title"] || "";
        meta.cover = meta.cover || grab["og:image"] || grab["twitter:image"] || "";
        meta.author = meta.author || grab["author"] || grab["article:author"] || "";
        if (grab.__icon) meta.logo = new URL(grab.__icon, page.origin).toString();
      }
    } catch { /* return whatever we have */ }

    // 3. Last resort for the publication name: derive it from the host.
    if (!meta.publication) {
      const host = page.hostname.replace(/^www\./, "");
      const stem = host.endsWith(".substack.com") ? host.replace(".substack.com", "") : host.split(".")[0];
      meta.publication = stem.replace(/[-_]/g, " ").replace(/\b\w/g, c => c.toUpperCase());
    }

    // Strip Substack's title suffix, e.g. "Post title - Publication".
    if (meta.publication && meta.title.endsWith(" - " + meta.publication)) {
      meta.title = meta.title.slice(0, -(meta.publication.length + 3));
    }

    return json(meta, 200, origin);
  }
};

function json(body, status, origin) {
  return new Response(JSON.stringify(body), { status, headers: cors(origin) });
}

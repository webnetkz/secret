export default defineNuxtConfig({
  ssr: false,
  css: ["~/assets/main.css"],
  runtimeConfig: {
    public: {
      apiBase: process.env.NUXT_PUBLIC_API_BASE || "http://192.168.0.15:3000/api",
      wsBase: process.env.NUXT_PUBLIC_WS_BASE || "ws://192.168.0.15:3000/ws"
    }
  },
  app: {
    head: {
      title: "Secret Chat Web",
      link: [
        { rel: "icon", type: "image/png", href: "/favicon.png" },
        {
          rel: "stylesheet",
          href: "https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css"
        }
      ]
    }
  },
  nitro: {
    compatibilityDate: "2026-02-18"
  }
})

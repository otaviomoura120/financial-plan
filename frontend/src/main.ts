import { createApp } from 'vue'

import App from '@/App.vue'
import { registerPlugins } from '@core/utils/plugins'

// Styles
import '@core/scss/template/index.scss'
import '@styles/styles.scss'
import { createAuth0 } from '@auth0/auth0-vue'


// Create vue app
const app = createApp(App)

app.use(
    createAuth0({
        domain: import.meta.env.VITE_AUTH0_DOMAIN,
        clientId: import.meta.env.VITE_AUTH0_CLIENT_ID,
        authorizationParams: {
            redirect_uri: window.location.origin,
            audience: 'https://financial-plan-api',
            scope: 'openid profile email offline_access'
        },
        useRefreshTokens: true
    })
)

// Register plugins
registerPlugins(app)

// Mount vue app
app.mount('#app')

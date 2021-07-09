import { createApp } from 'vue'
import App from './App.vue'
import urql from '@urql/vue';

const app = createApp(App)

app.use(urql, {
    url: window.location.origin + '/graphql',
});

app.mount('#app')

import { Client } from '@stomp/stompjs';
import { ref } from 'vue';
import type { Threat } from 'src/types/threat';
import { useAuthStore } from 'src/stores/auth';
import { useQuasar } from 'quasar';

export function useWebSocket() {
  const threats = ref<Threat[]>([]);
  const connected = ref(false);
  const $q = useQuasar()
  const authStore = useAuthStore()

  function connect() {
    const client = new Client({
      brokerURL: 'ws://localhost:8080/ws',
    })

    client.onConnect = () => {
      connected.value = true;
      client.subscribe('/topic/threats', (message) => {
        const threat = JSON.parse(message.body) as Threat;
        threats.value.unshift(threat);
      });
      client.subscribe(`/topic/alerts/${authStore.user?.id}`, (message) => {
        const alert = JSON.parse(message.body)
        $q.notify({
          type: 'warning',
          message: `New alert: ${alert.title}`,
          caption: alert.severity,
          position: 'top-right',
        })
      })
    }

    client.activate();
  }

  return { threats, connected, connect};
}

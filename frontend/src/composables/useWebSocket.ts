import { Client } from '@stomp/stompjs';
import { onBeforeUnmount, ref } from 'vue';
import type { ApiThreat, Threat } from 'src/types/threat';
import { mapThreat } from 'src/services/threats.service';
import { useAuthStore } from 'src/stores/auth';
import { useQuasar } from 'quasar';

// The list is shown on a dashboard, so it only needs the latest few entries
const MAX_LIVE_THREATS = 50;

export function useWebSocket() {
  const threats = ref<Threat[]>([]);
  const connected = ref(false);
  const $q = useQuasar()
  const authStore = useAuthStore()
  let client: Client | null = null;

  function connect() {
    // A second call must not open a second connection
    if (client) return;

    client = new Client({
      brokerURL: import.meta.env.VITE_WS_URL ?? 'ws://localhost:8080/ws',
      // The server checks the same token as for the REST API
      connectHeaders: {
        Authorization: `Bearer ${authStore.accessToken ?? localStorage.getItem('token') ?? ''}`,
      },
    })

    client.onConnect = () => {
      connected.value = true;
      client?.subscribe('/topic/threats', (message) => {
        const apiThreat = JSON.parse(message.body) as ApiThreat;
        threats.value.unshift(mapThreat(apiThreat));
        if (threats.value.length > MAX_LIVE_THREATS) {
          threats.value.length = MAX_LIVE_THREATS;
        }
      });

      // Without a user id the topic name would be wrong and nothing would ever arrive
      const userId = authStore.user?.id;
      if (userId) {
        client?.subscribe(`/topic/alerts/${userId}`, (message) => {
          const alert = JSON.parse(message.body)
          $q.notify({
            type: 'warning',
            message: `New alert: ${alert.title}`,
            caption: alert.severity,
            position: 'top-right',
          })
        })
      }
    }

    client.onStompError = (frame) => {
      // An old or invalid token would be refused again on every reconnect, so stop trying
      if (frame.headers['message']?.includes('Unauthorized')) {
        disconnect();
      }
    }

    client.onDisconnect = () => {
      connected.value = false;
    }

    client.activate();
  }

  function disconnect() {
    if (!client) return;
    void client.deactivate();
    client = null;
    connected.value = false;
  }

  // Leaving the page must close the socket, or every visit leaves one connection behind
  onBeforeUnmount(disconnect);

  return { threats, connected, connect, disconnect };
}

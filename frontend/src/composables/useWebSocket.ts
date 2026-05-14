import { Client } from '@stomp/stompjs';
import { ref } from 'vue';
import type { Threat } from 'src/types/threat';

export function useWebSocket() {
  const threats = ref<Threat[]>([]);
  const connected = ref(false);

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
    }

    client.activate();
  }

  return { threats, connected, connect};
}

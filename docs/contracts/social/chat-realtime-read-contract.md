# Friend chat realtime and read contract

Status: DRAFT · Version: 1 · 2026-10-03
Base: BE `b91d3639f911ec409bffcf77a77b2e9a9f994347` (implementation SHA pending)
Preview: `http://127.0.0.1:19081` · FE origin: `http://127.0.0.1:13005`

## Browser transport and authority

Use SockJS at `/ws` and STOMP. The HTTP handshake is allowed only for the configured origin; send `Authorization: Bearer <access JWT>` in the STOMP **CONNECT** headers. Never put a token in a URL. Wait for CONNECTED before SUBSCRIBE. Each session is bound to the JWT's user and player; client sender IDs are ignored. Expired/deactivated sessions and revoked channel membership stop receiving events. FE disconnects the STOMP session on logout; the current auth API has no server-side logout/revocation endpoint for an otherwise valid JWT. Subscribe only to `/topic/social/chat/{channelId}` after opening a channel. The server checks active participation on SUBSCRIBE and SEND. Client SEND to broker destinations (`/topic`, `/queue`) is forbidden.

FE sends a message with **one** `POST /api/v1/chat/channels/{channelId}/messages` request (`Authorization: Bearer`, JSON `{ "content": "...", "clientMessageId": "stable UUID" }`). Do not also STOMP SEND. Legacy `/app/social/chat/{channelId}/send` remains subject to the same authorization and persistence rules. `clientMessageId` is stable across retries for one logical message. Same channel + sender + key + body returns the original message ID and emits no second event; same key with a different body returns 409. Requests without a key remain accepted for older clients, without retry deduplication. A successful REST response confirms DB commit; realtime delivery is best effort. The DB is the recovery source.

The REST response's `result.id` and realtime `id` are the same server message ID. A realtime MESSAGE contains `eventType: MESSAGE_CREATED`, `id`, `channelId`, `senderId`, `content`, `edited`, `createdAt`, and `clientMessageId` (nullable for old sends). FE deduplicates by `id` and orders by server message ID for display, while using recovery pages to fill gaps.

## Recovery and read

`GET /api/v1/chat/channels/{channelId}/messages?cursor=<exclusive ID>&size=50` returns ascending `messages`, `hasMore`, `nextCursor`; an absent cursor starts at newest. Follow `nextCursor` until `hasMore=false`, merge pages by ID, then refetch the newest page to cover writes committed during pagination. Reconcile live events by ID. The first page and later pages can overlap with concurrent writes; deduplicate. IDs are pagination keys, not proof of commit order. Existing message history stays readable after a friend block, while new sends are forbidden.

Friend channel list `GET /api/v1/chat/channels/friends` includes `lastReadMessageId` (nullable), `peerLastReadMessageId` (nullable), and `unreadCount`. `POST /api/v1/chat/channels/{channelId}/read` with `{ "lastReadMessageId": <visible server message ID> }` marks only through that displayed message, returns the updated read state, and emits `eventType: READ_UPDATED`, `channelId`, `playerId`, `lastReadMessageId` to the same subscription. GET never marks read. The position only advances, cannot point to an absent or foreign-channel message, and excludes own sends from unread count. New and existing participants start with null read positions; old messages are not deemed read. Only FRIEND channels support this read API. Nonparticipants get 403; invalid target gets 400/404; blocked friends retain history/read access.

Verification accounts: `/Users/ryu/.local/share/lifeasgame-demo/lag-demo-129fd1f60637/namespaces/chat-realtime-20261003/credentials.json` (0600); the same path is recorded in `backend-next.json`. No credentials belong in this contract.

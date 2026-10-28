# Sum Web Service on an Active Directory Domain

A minimal **REST web service** written in plain Java (no frameworks) that runs on a Windows Server domain controller (`DC01`) and is called by a domain-joined Ubuntu client (`client1`).

This project is a follow-up to my Active Directory lab: **[link to your AD repository]**, where `client1` was joined to the `akrem.local` domain.

## Architecture

```
 client1 (Ubuntu, joined to akrem.local)         DC01 (Windows Server, 192.168.56.10)
 ┌────────────────────┐   HTTP GET/POST :5000    ┌──────────────────────────────┐
 │ SumClient / curl   │ ───────────────────────► │ Windows Firewall (rule: 5000)│
 │                    │ ◄─────────────────────── │ SumService (Java HttpServer) │
 └────────────────────┘      JSON response       └──────────────────────────────┘
```

Both machines are on a VirtualBox host-only network (`192.168.56.0/24`). The client finds the server through the domain's DNS (`dc01.akrem.local`).

## Features

- `GET /sum?a=5&b=7` and `POST /sum` with a JSON body
- JSON responses, with `400` on invalid input and `405` on other HTTP methods
- Thread pool (4 threads) so several clients can be served at once
- Only the JDK is needed: `com.sun.net.httpserver` for the server and `java.net.http.HttpClient` for the client

## API

| Method | Request | Response |
|---|---|---|
| GET | `/sum?a=5&b=7` | `{"a":5.0,"b":7.0,"sum":12.0}` |
| POST | `/sum` with body `{"a": 10, "b": 20}` | `{"a":10.0,"b":20.0,"sum":30.0}` |
| any other | | `405 {"error":"Use GET or POST"}` |
| bad input | | `400 {"error":"Provide numeric values a and b"}` |

## Requirements

- JDK 11 or later on both machines (tested with JDK 25)
- A domain controller reachable from the client (here `DC01`, 192.168.56.10)

## Setup and run

### 1. On DC01: allow the port through the firewall

Run PowerShell as Administrator:

```powershell
New-NetFirewallRule -DisplayName "Sum Web Service" -Direction Inbound -Protocol TCP -LocalPort 5000 -Action Allow
```

### 2. On DC01: start the service

```powershell
cd src
java SumService.java
```

Test it locally:

```powershell
Invoke-RestMethod "http://localhost:5000/sum?a=5&b=7"
```

Stop it with `Ctrl+C`. This closes the port; the firewall rule stays until you remove it.

### 3. On client1: call the service

```bash
curl "http://dc01.akrem.local:5000/sum?a=5&b=7"

curl -X POST http://dc01.akrem.local:5000/sum \
  -H "Content-Type: application/json" \
  -d '{"a": 5, "b": 7}'

java src/SumClient.java
```

Expected output of `SumClient`:

```
GET  -> 200 {"a":5.0,"b":7.0,"sum":12.0}
POST -> 200 {"a":10.0,"b":20.0,"sum":30.0}
```

> Always put the URL in quotes in a Linux shell. An unquoted `&` sends the command to the background and drops `b=7`.

## Troubleshooting

| Symptom | Cause and fix |
|---|---|
| Works on DC01 with `localhost` but **times out** from client1 | Windows Firewall silently drops the packets. Create the inbound rule for TCP 5000 (step 1). `localhost` traffic never passes through the firewall, which is why the local test works. |
| `Connection refused` | The firewall is fine but the service is not running. Start `SumService`. |
| Listening only on `127.0.0.1` | The server must bind to `0.0.0.0`. Check with `netstat -ano \| findstr :5000`. |
| `Could not resolve host` | Check DNS on client1: `nslookup dc01.akrem.local`, or use `192.168.56.10`. |
| `Address already in use` | Another process holds port 5000. Find it with `netstat -ano \| findstr :5000` and stop it. |
| `curl` returns only `a` | Missing quotes around the URL (see the note above). |
| Port check from client1 | `nc -zv 192.168.56.10 5000` |

## Key concepts

- **Web service**: a function exposed over the network, called through HTTP, with data exchanged in a language-independent format (JSON).
- **REST**: a style of web service built on URLs and HTTP methods (GET, POST), lighter than SOAP/WSDL.
- **Socket / port**: HTTP runs over TCP. The server opens a listening socket on port 5000 and each client opens a connection to it.
- **Thread pool**: a fixed set of worker threads that handle incoming requests concurrently.
- **Inbound firewall rule**: Windows blocks incoming connections to any port that has no allow rule.

## Project structure

```
sum-web-service/
├── src/
│   ├── SumService.java   # server (run on DC01)
│   └── SumClient.java    # client (run on client1)
├── README.md
└── .gitignore
```

## Possible extensions

- SOAP version with a WSDL (JAX-WS)
- Version using raw `ServerSocket` / `Socket`
- Spring Boot version
- Run the service as a Windows service so it survives logoff

## Notes

This is a learning lab. In production, do not host extra services on a domain controller, and use HTTPS and authentication.

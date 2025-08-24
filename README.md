# 🛠️ FixLite — Lightweight FIX Engine for High-Performance Trading Systems

**FixLite** is a minimalist, high-performance FIX protocol engine built in **Java 21**, designed for **native-image compatibility** using **GraalVM**.  
It uses **Netty** for ultra-fast, low-latency TCP communication and implements a simplified FIX session manager for fast onboarding and execution of FIX messages like **Logon (35=A)** and **NewOrderSingle (35=D)**.

---

## 🔑 Key Features

- ✅ Fully compatible with **GraalVM Native Image**
- ⚡ Built on **Netty** for high-performance TCP networking
- 🧩 Simple, statically defined **FIX session model**
- 🚀 Minimal runtime overhead — no reflection, no external configuration
- 🧪 Easily extendable to support additional message types (e.g., `ExecutionReport`, `Cancel`, etc.)
- 🐳 Includes **native-compatible Dockerfile**
- 🔄 Automatic heartbeat management and sequence number validation
- 📊 Built-in order management and execution simulation

---

## 🧠 Use Cases

- Proprietary trading systems  
- FIX gateways in microservices  
- Embedded trading simulators  
- Low-latency FIX benchmarking
- Testing and development of FIX-based applications

---

## 🚀 Quick Start

### Prerequisites

- **Java 21** or higher
- **Maven 3.8+**
- **GraalVM 22.3+** (for native image compilation)
- **Docker** (optional, for containerized deployment)

### Building the Project

```bash
# Clone the repository
git clone <repository-url>
cd fixlite

# Build with Maven
mvn clean package

# Or build native image
mvn clean package -Pnative -DskipTests
```

### Running the Gateway

#### Option 1: Java JAR
```bash
java -jar target/fixlite-gateway-1.0.0-SNAPSHOT.jar [port]
```

#### Option 2: Native Binary
```bash
./target/fixlite-gateway [port]
```

#### Option 3: Docker
```bash
docker-compose up --build
```

Default port is **9878** (standard FIX port).

### Testing with Client

```bash
# Run the test client
java -cp target/fixlite-gateway-1.0.0-SNAPSHOT.jar com.fixlite.client.FixClient
```

The client provides an interactive shell for:
- Sending logon/logout messages
- Submitting orders
- Sending heartbeats
- Receiving execution reports

---

## 📋 Supported FIX Messages

### Session Level
- **Logon (A)** - Session establishment
- **Logout (5)** - Session termination
- **Heartbeat (0)** - Keep-alive mechanism
- **Test Request (1)** - Connection testing
- **Reject (3)** - Message rejection

### Application Level
- **New Order Single (D)** - Order submission
- **Execution Report (8)** - Order status and fills
- **Order Cancel Request (F)** - Order cancellation
- **Order Cancel Reject (9)** - Cancel rejection
- **Business Message Reject (j)** - Application-level rejection

---

## 🏗️ Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   FIX Client    │────│  FixLite Gateway │────│ Message Processor│
└─────────────────┘    └──────────────────┘    └─────────────────┘
                              │
                       ┌──────────────────┐
                       │ Session Manager  │
                       └──────────────────┘
                              │
                       ┌──────────────────┐
                       │   FIX Sessions   │
                       └──────────────────┘
```

### Core Components

- **`FixLiteGateway`** - Main server using Netty
- **`FixSession`** - Individual FIX session management
- **`FixSessionManager`** - Multi-session coordination
- **`FixMessageProcessor`** - Application message handling
- **`FixMessage`** - FIX message representation
- **`FixParser`** - High-performance message parsing

---

## 🔧 Configuration

### Environment Variables

- `FIX_PORT` - Gateway port (default: 9878)
- `JAVA_OPTS` - JVM options for containerized deployment

### Logging

Logging is configured via `logback.xml`:
- Console output for development
- File rotation for production
- Configurable log levels per package

---

## 🐳 Docker Deployment

### Build and Run
```bash
# Build and start
docker-compose up --build

# Run in background
docker-compose up -d

# View logs
docker-compose logs -f

# Stop
docker-compose down
```

### Native Image Container
The Dockerfile creates a minimal container with the native binary:
- Multi-stage build with GraalVM
- Minimal runtime image (UBI8)
- Non-root user execution
- Health checks included

---

## 📊 Performance

### Benchmarks (Preliminary)
- **Latency**: Sub-millisecond message processing
- **Throughput**: 10,000+ messages/second
- **Memory**: <50MB native image footprint
- **Startup**: <100ms native image cold start

### Optimizations
- Zero-copy message parsing where possible
- Minimal object allocation
- Lock-free sequence number management
- Netty's high-performance I/O

---

## 🧪 Testing

### Unit Tests
```bash
mvn test
```

### Integration Testing
1. Start the gateway: `java -jar target/fixlite-gateway-1.0.0-SNAPSHOT.jar`
2. Run client: `java -cp target/fixlite-gateway-1.0.0-SNAPSHOT.jar com.fixlite.client.FixClient`
3. Test commands:
   ```
   > order AAPL 1 100 150.50  # Buy 100 AAPL at $150.50
   > order MSFT 2 50 300.25   # Sell 50 MSFT at $300.25
   > heartbeat                # Send heartbeat
   > logout                   # Disconnect
   ```

---

## 🔮 Roadmap

- [ ] **Market Data Support** - Level 1 and Level 2 data
- [ ] **Order Book Management** - Full order book simulation
- [ ] **Risk Management** - Position and exposure limits
- [ ] **Persistence** - Session and order state recovery
- [ ] **Clustering** - Multi-node deployment support
- [ ] **Metrics** - Prometheus/Grafana integration
- [ ] **FIX 5.0 Support** - Latest protocol version

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

- **Netty** - High-performance networking framework
- **GraalVM** - Native image compilation
- **QuickFIX** - Inspiration for FIX protocol implementation
- **FIX Protocol** - Financial Information eXchange standard

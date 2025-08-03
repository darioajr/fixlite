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

---

## 🧠 Use Cases

- Proprietary trading systems  
- FIX gateways in microservices  
- Embedded trading simulators  
- Low-latency FIX benchmarking

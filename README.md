# Awki: Plataforma de Telemonitorización y Triaje Obstétrico con IA

**Awki** es una solución SaaS B2B2C diseñada para erradicar el "vacío de datos" clínico entre los controles prenatales. Al descentralizar el monitoreo mediante una arquitectura *Offline-First* y el uso de Procesamiento de Lenguaje Natural (NLP), Awki permite la identificación temprana de complicaciones obstétricas (como la preeclampsia) en zonas con alta saturación del sistema de salud.

## Arquitectura del Sistema

El sistema está compuesto por dos entornos principales que operan de forma bidireccional:

1. **Frontend Paciente (PWA):** Interfaz conversacional orientada al consumidor final. Permite el ingreso de datos a través de lenguaje natural. La IA procesa el contexto del chat, clasifica la información (síntomas, estado de ánimo, niveles de dolor) y autocompleta el historial de la paciente de forma invisible.
2. **Dashboard Médico (SaaS B2B):** Panel de control de alta disponibilidad para especialistas. Recibe los datos estructurados, ejecuta un motor de reglas para el triaje predictivo (Semaforización de Riesgo) y emite alertas tempranas sin necesidad de que el médico audite los chats individuales.

## Características Principales (Core Features)

* **Motor de Extracción NLP:** Análisis semántico de conversaciones para la detección automatizada de variables clínicas en tiempo real.
* **Triaje Predictivo Automatizado:** Clasificación dinámica del riesgo del paciente basada en la frecuencia y severidad de los síntomas reportados.
* **Estrategia FinOps (Caché Semántica):** Implementación de vectorización de consultas frecuentes para reducir la latencia y los costos operativos de la API del LLM a cero en interacciones repetitivas.
* **Resiliencia Offline-First:** Sincronización asíncrona de datos locales para garantizar la operatividad en áreas geográficas con conectividad intermitente.
* **Sistema de Alertas (Pub/Sub):** Notificaciones bidireccionales y botón de emergencia SOS con geolocalización de centros de salud especializados.

## Stack Tecnológico (Propuesto)

* **Frontend:** React / TypeScript / PWA
* **Backend:** Spring Boot / Java (o Node.js / TypeScript)
* **Base de Datos:** PostgreSQL (Relacional para historiales clínicos) + Redis (Caché semántica)
* **Integraciones:** API LLM (Procesamiento de Lenguaje Natural), Google Maps API / Places API (Geolocalización médica)
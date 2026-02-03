# FisioterApp - Backend Server (Ktor + Exposed)

![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-purple?style=for-the-badge&logo=kotlin)
![Ktor](https://img.shields.io/badge/Ktor-2.3.7-blue?style=for-the-badge&logo=ktor)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-336791?style=for-the-badge&logo=postgresql)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker)
![Status](https://img.shields.io/badge/Status-MVP_Ready-success?style=for-the-badge)

> Backend RESTful para la gestión integral de clínicas de fisioterapia. Diseñado para soportar aplicaciones móviles multiplataforma (KMP) con flujos de auto-registro de pacientes y expedientes clínicos electrónicos.

---

## Arquitectura y Lógica de Negocio

El sistema implementa una lógica de **Onboarding Progresivo** para reducir la carga administrativa del fisioterapeuta:

1.  **Rol Guest (Pre-Registro):** El usuario descarga la App y crea una cuenta. Llena su "Formulario Inicial" (Antecedentes y Objetivos) desde casa.
2.  **Validación Clínica:** El fisioterapeuta recibe al paciente, valida sus datos y crea el "Expediente Oficial" (`Patient`).
3.  **Vinculación:** El sistema enlaza la cuenta de Usuario (`UserTable`) con el Expediente Clínico (`PatientTable`), otorgando acceso al paciente para ver su progreso y tareas.

### Diagrama de Entidad-Relación (Simplificado)
* **Users:** Credenciales y Roles (GUEST, ACTIVE_PATIENT, PHYSIO).
* **IntakeForms:** Datos temporales llenados por el usuario (Borrador).
* **Patients:** Expediente validado (Vinculado a User).
* **Sessions:** Bitácora diaria (SOAP) y estatus de pago.
* **FunctionalAssessments:** Escalas geriátricas (Barthel/Katz).

---

## Tech Stack

* **Lenguaje:** Kotlin 100%
* **Framework Web:** Ktor Server (Netty Engine)
* **Base de Datos:** PostgreSQL 15
* **ORM:** JetBrains Exposed (DAO & DSL)
* **Serialización:** Kotlinx.Serialization (JSON)
* **Infraestructura:** Docker Compose
* **Manejo de Fechas:** `java.time` (ISO-8601)

---

## Instalación y Despliegue

### Prerrequisitos
* JDK 17+
* Docker Desktop instalado y corriendo.

### 1. Clonar el repositorio
bash
git clone [https://github.com/tu-usuario/fisioterapp-backend.git](https://github.com/tu-usuario/fisioterapp-backend.git)
cd fisioterapp-backend

## 2. Configurar Base de Datos
El proyecto incluye un `docker-compose.yml` preconfigurado.

* **Usuario:** admin_fisio
* **Password:** password_seguro_123
* **DB Name:** fisioterapp_db
* **Puerto:** 5432

Levanta el servicio de base de datos:

bash
docker-compose up -d
##3. Ejecutar el Servidor
Bash

./gradlew run
El servidor iniciará en http://0.0.0.0:8080. Al iniciar, Exposed creará automáticamente las tablas si no existen.
## Documentación de Endpoints
## Autenticación (/auth)
Método	Ruta	Body (JSON)	Descripción
POST	/register	{ "email": "...", "password": "...", "role": "GUEST" }	Registro de nuevo usuario.
POST	/login	{ "email": "...", "password": "..." }	Retorna Token y UserDTO.

## Onboarding Paciente (/intake-forms)
Método	Ruta	Descripción
POST	/	Guarda el formulario de antecedentes llenado por el Guest.
GET	/user/{userId}	El Fisio recupera el formulario usando el ID de Usuario para validarlo.

## Gestión de Pacientes (/patients)
Método	Ruta	Descripción
GET	/	Lista todos los pacientes.
POST	/	Crea un paciente oficial. Nota: Si se envía userId, vincula la cuenta y actualiza el rol a ACTIVE_PATIENT.
POST	/import	Carga Masiva (CSV): Sube un archivo con formato `Nombre

## Expediente Clínico (/patients/{id}/...)
Todas estas rutas son anidadas bajo un ID de paciente específico.

Historia Clínica: GET / POST -> .../medical-history

Maneja antecedentes y revisión por sistemas.

Evaluaciones: GET / POST -> .../evaluations

Diagnósticos, peso, talla, IMC y objetivos.

Sesiones: GET / POST -> .../sessions

Bitácora diaria, notas SOAP y planificación de próxima cita.

Funcionalidad (Adulto Mayor): GET / POST -> .../functional-assessments

Escalas numéricas (Comer, Bañarse, Movilidad) para graficar progreso.

## Testing Rápido (cURL)
Puedes probar el flujo completo desde tu terminal:

1. Registrar Usuario

Bash

curl -X POST http://localhost:8080/auth/register -H "Content-Type: application/json" -d '{"email":"test@user.com","password":"123","role":"GUEST"}'
2. Enviar Formulario de Salud

Bash

curl -X POST http://localhost:8080/intake-forms -H "Content-Type: application/json" -d '{"userId": 1, "fullName": "Juan Perez", "birthDate": "1990-01-01", "phone": "555555", "sysMusculoskeletal": true}'
3. Activar Paciente (Vinculación)

Bash

curl -X POST http://localhost:8080/patients -H "Content-Type: application/json" -d '{"userId": 1, "name": "Juan Perez", "dob": "1990-01-01", "gender": "M", "occupation": "Dev", "phone": "555555"}'
# Estructura del Proyecto

com.logikamobile.fisioterapp
├── Application.kt          # Entry Point & Plugins (Koin, Serialization, CORS)
├── Databases.kt            # Configuración de Exposed y Tablas SQL
├── Routing.kt              # Definición de todos los Endpoints
└── model
├── DTOs.kt             # Data Classes serializables (JSON)
└── Enums.kt            # Roles y Niveles de Actividad
## Autor
Desarrollado por Luis Daniel Michel Peña. Proyecto creado como parte del portafolio Full Stack Developer con especialización en Kotlin.

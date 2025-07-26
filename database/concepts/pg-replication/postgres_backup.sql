--
-- PostgreSQL database dump
--

-- Dumped from database version 16.9 (Debian 16.9-1.pgdg120+1)
-- Dumped by pg_dump version 16.9 (Debian 16.9-1.pgdg120+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: replicated_table; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.replicated_table (
    id integer NOT NULL,
    message text
);


ALTER TABLE public.replicated_table OWNER TO postgres;

--
-- Name: replicated_table_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.replicated_table_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.replicated_table_id_seq OWNER TO postgres;

--
-- Name: replicated_table_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.replicated_table_id_seq OWNED BY public.replicated_table.id;


--
-- Name: replicated_table id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.replicated_table ALTER COLUMN id SET DEFAULT nextval('public.replicated_table_id_seq'::regclass);


--
-- Data for Name: replicated_table; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.replicated_table (id, message) FROM stdin;
1	First Message
2	Second Message
\.


--
-- Name: replicated_table_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.replicated_table_id_seq', 2, true);


--
-- Name: replicated_table replicated_table_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.replicated_table
    ADD CONSTRAINT replicated_table_pkey PRIMARY KEY (id);


--
-- PostgreSQL database dump complete
--


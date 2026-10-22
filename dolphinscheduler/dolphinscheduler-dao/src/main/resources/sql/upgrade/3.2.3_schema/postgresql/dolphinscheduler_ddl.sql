ALTER TABLE public.t_ds_audit_log ADD http_method varchar NULL;
COMMENT ON COLUMN public.t_ds_audit_log.http_method IS 'http获取请求方法';
# Production Deployment Runbook

**Complete step-by-step guide for deploying Water Access Optimizer to production.**

---

## Pre-Deployment Checklist (1 Week Before)

### Code Quality
- [ ] All tests passing (unit, integration, E2E)
- [ ] Code coverage ≥80%
- [ ] SonarQube quality gate passed
- [ ] No HIGH/CRITICAL security vulnerabilities
- [ ] Code review approved by 2+ developers

### Documentation
- [ ] CHANGELOG.md updated
- [ ] API documentation current
- [ ] Deployment notes documented
- [ ] Rollback procedure tested

### Infrastructure
- [ ] Kubernetes cluster ready (3+ nodes)
- [ ] Database backup verified
- [ ] Secrets stored in Vault
- [ ] SSL certificates valid
- [ ] DNS configured

### Communication
- [ ] Stakeholders notified (3 days notice)
- [ ] Maintenance window scheduled
- [ ] On-call engineer assigned
- [ ] Communication channels ready (Slack, email)

---

## Deployment Day Timeline

### T-30 minutes: Final Preparation

```bash
# 1. Verify staging deployment is healthy
kubectl get pods -n water-optimizer-staging
kubectl logs deployment/auth-service -n water-optimizer-staging --tail=100

# 2. Take final database backup
kubectl exec -it postgres-0 -n water-optimizer -- \
  pg_dump -U wateradmin wateraccess > backup_$(date +%Y%m%d_%H%M%S).sql

# 3. Verify backup
ls -lh backup_*.sql

# 4. Upload backup to S3/GCS
aws s3 cp backup_*.sql s3://wateroptimizer-backups/$(date +%Y%m%d)/

# 5. Tag release
git tag -a v1.0.0 -m "Release v1.0.0 - Production ready"
git push origin v1.0.0
```

### T-0: Start Deployment

#### Phase 1: Deploy Infrastructure Updates (if any)

```bash
# Apply ConfigMaps
kubectl apply -f k8s/configmaps/ -n water-optimizer

# Apply Secrets (if changed)
# Using Vault
vault kv put secret/water-optimizer/database password="new_password"

# Or using Sealed Secrets
kubectl apply -f k8s/secrets/sealed-secret.yaml -n water-optimizer
```

#### Phase 2: Deploy Backend Services (Canary)

```bash
# Deploy to 10% of pods (canary)
kubectl set image deployment/auth-service \
  auth-service=your-registry/auth-service:v1.0.0 \
  -n water-optimizer

kubectl set image deployment/water-integrator \
  water-integrator=your-registry/water-integrator:v1.0.0 \
  -n water-optimizer

kubectl set image deployment/water-visualizer \
  water-visualizer=your-registry/water-visualizer:v1.0.0 \
  -n water-optimizer

# Scale to create canary deployment (10% of traffic)
kubectl scale deployment/auth-service --replicas=3 -n water-optimizer
# (Assuming 2 old pods + 1 new pod = 33% new, adjust as needed)
```

### T+5 minutes: Monitor Canary

```bash
# Watch pod status
kubectl get pods -n water-optimizer --watch

# Check logs for errors
kubectl logs -f deployment/auth-service -n water-optimizer

# Monitor metrics in Grafana
open https://grafana.wateroptimizer.org

# Check error rate (should be <1%)
# Check P95 latency (should be <500ms)
# Check CPU/memory usage

# If ANY issues, ROLLBACK immediately
kubectl rollout undo deployment/auth-service -n water-optimizer
```

### T+15 minutes: Scale to 50%

```bash
# If canary successful, scale to 50%
kubectl scale deployment/auth-service --replicas=6 -n water-optimizer
kubectl scale deployment/water-integrator --replicas=6 -n water-optimizer
kubectl scale deployment/water-visualizer --replicas=4 -n water-optimizer
```

### T+30 minutes: Full Rollout

```bash
# Deploy to 100%
kubectl rollout status deployment/auth-service -n water-optimizer
kubectl rollout status deployment/water-integrator -n water-optimizer
kubectl rollout status deployment/water-visualizer -n water-optimizer

# Verify all pods running
kubectl get pods -n water-optimizer

# Check service endpoints
curl https://api.wateroptimizer.org/actuator/health
# Expected: {"status":"UP"}
```

#### Phase 3: Deploy Frontend

```bash
# Deploy frontend
kubectl set image deployment/frontend \
  frontend=your-registry/frontend:v1.0.0 \
  -n water-optimizer

# Wait for rollout
kubectl rollout status deployment/frontend -n water-optimizer

# Test frontend
curl https://wateroptimizer.org
# Expected: HTTP 200

# Test in browser
open https://wateroptimizer.org
# Expected: Application loads, login works
```

#### Phase 4: Deploy API Gateway

```bash
# Deploy gateway (last to ensure backends are ready)
kubectl set image deployment/api-gateway \
  api-gateway=your-registry/api-gateway:v1.0.0 \
  -n water-optimizer

# Monitor rollout
kubectl rollout status deployment/api-gateway -n water-optimizer
```

### T+60 minutes: Post-Deployment Verification

```bash
# Run smoke tests
./scripts/smoke-tests.sh production

# Check all health endpoints
for service in auth-service water-integrator water-visualizer; do
  echo "Checking $service..."
  kubectl exec -it deployment/$service -n water-optimizer -- \
    curl localhost:8080/actuator/health
done

# Verify database connections
kubectl exec -it postgres-0 -n water-optimizer -- \
  psql -U wateradmin -d wateraccess -c \
  "SELECT COUNT(*) FROM pg_stat_activity WHERE state = 'active';"

# Check Redis
kubectl exec -it redis-0 -n water-optimizer -- \
  redis-cli INFO stats

# Test critical user flows
# 1. User registration
# 2. Login
# 3. Data upload
# 4. Risk assessment
# 5. Map visualization
```

### T+90 minutes: Monitor

**Watch Grafana Dashboards** for 1 hour:
- Error rate (target: <1%)
- P95 latency (target: <500ms)
- CPU usage (target: <70%)
- Memory usage (target: <80%)
- Database connection pool (target: <80%)

**Check Logs** for errors:
```bash
# Tail all services
kubectl logs -f --prefix deployment/auth-service -n water-optimizer
kubectl logs -f --prefix deployment/water-integrator -n water-optimizer
```

**Alert Thresholds**:
- Error rate >5% for 5 minutes → ROLLBACK
- P95 latency >2s for 10 minutes → ROLLBACK
- Critical error in logs → ROLLBACK

---

## Rollback Procedure (15 minutes)

### When to Rollback

**Immediate rollback if**:
- Error rate >5%
- Service unavailable >5 minutes
- Data corruption detected
- Critical security vulnerability
- P95 latency >2s

### Rollback Steps

```bash
# 1. Alert team
# Post in #incidents Slack channel: "Rolling back v1.0.0 due to [reason]"

# 2. Rollback deployments
kubectl rollout undo deployment/api-gateway -n water-optimizer
kubectl rollout undo deployment/auth-service -n water-optimizer
kubectl rollout undo deployment/water-integrator -n water-optimizer
kubectl rollout undo deployment/water-visualizer -n water-optimizer
kubectl rollout undo deployment/frontend -n water-optimizer

# 3. Verify rollback
kubectl rollout status deployment/auth-service -n water-optimizer

# 4. Check services are healthy
for service in auth-service water-integrator water-visualizer; do
  kubectl exec -it deployment/$service -n water-optimizer -- \
    curl localhost:8080/actuator/health
done

# 5. Monitor for 30 minutes
watch kubectl get pods -n water-optimizer

# 6. (If needed) Rollback database migrations
# Only if database schema changed
kubectl exec -it postgres-0 -n water-optimizer -- \
  psql -U wateradmin -d wateraccess < rollback_migration.sql

# 7. Verify application works
curl https://api.wateroptimizer.org/actuator/health
open https://wateroptimizer.org

# 8. Document incident
# Create post-mortem document in docs/incidents/
```

---

## Post-Deployment (24 Hours)

### Hour 1-2: Intensive Monitoring

```bash
# Watch Grafana dashboards continuously
open https://grafana.wateroptimizer.org

# Check error logs every 15 minutes
kubectl logs --since=15m deployment/auth-service -n water-optimizer | grep ERROR

# Monitor user feedback
# Check support email, Slack, GitHub issues
```

### Hour 3-6: Periodic Checks

```bash
# Check metrics every hour
# Error rate, latency, resource usage

# Review logs for warnings
kubectl logs --since=1h deployment/auth-service -n water-optimizer | grep WARN
```

### Hour 7-24: Background Monitoring

- Grafana dashboards on display
- PagerDuty alerts configured
- On-call engineer available

### Day 1: Retrospective

**Schedule 1-hour meeting with team**:
1. What went well?
2. What could be improved?
3. Action items for next deployment
4. Update runbook with learnings

**Update Documentation**:
- [ ] Document any issues encountered
- [ ] Update runbook with new steps
- [ ] Update troubleshooting guide
- [ ] Add to FAQ if needed

---

## Emergency Procedures

### Database Emergency

**Symptoms**: Database unavailable, connection errors

```bash
# Check pod status
kubectl get pods -l app=postgres -n water-optimizer

# Check logs
kubectl logs postgres-0 -n water-optimizer

# Restart pod
kubectl delete pod postgres-0 -n water-optimizer
# StatefulSet will recreate

# If corrupt, restore from backup
kubectl exec -it postgres-0 -n water-optimizer -- \
  psql -U wateradmin -d wateraccess < backup_latest.sql
```

### Memory Leak

**Symptoms**: Pods restarting, OOMKilled errors

```bash
# Check pod events
kubectl describe pod auth-service-xxx -n water-optimizer

# Increase memory limit (temporary)
kubectl set resources deployment/auth-service \
  --limits=memory=2Gi \
  -n water-optimizer

# Investigate heap dump
kubectl exec -it auth-service-xxx -n water-optimizer -- \
  jmap -dump:format=b,file=/tmp/heap.hprof 1

# Copy heap dump locally
kubectl cp water-optimizer/auth-service-xxx:/tmp/heap.hprof ./heap.hprof

# Analyze with Eclipse MAT
```

### High Traffic Spike

**Symptoms**: Slow responses, high CPU

```bash
# Check current load
kubectl top pods -n water-optimizer
kubectl top nodes

# Scale horizontally
kubectl scale deployment/auth-service --replicas=10 -n water-optimizer
kubectl scale deployment/water-integrator --replicas=8 -n water-optimizer

# Enable rate limiting
kubectl apply -f k8s/rate-limiting.yaml -n water-optimizer

# Monitor auto-scaling
kubectl get hpa -n water-optimizer --watch
```

---

## Deployment Metrics

### Track for Each Deployment

**Deployment Speed**:
- Time from tag creation to production live
- Target: <60 minutes

**Reliability**:
- Deployment success rate
- Target: >95%

**Rollback Rate**:
- % of deployments requiring rollback
- Target: <5%

**Downtime**:
- Minutes of downtime during deployment
- Target: 0 (zero-downtime deployments)

**Time to Recovery**:
- Minutes from issue detection to rollback complete
- Target: <15 minutes

---

## Contacts

### Escalation Path

1. **On-Call Engineer**: [Your Number]
2. **DevOps Lead**: [Lead Number] (if >30 min)
3. **Engineering Manager**: [Manager Number] (if >1 hour)
4. **CTO**: [CTO Number] (critical only)

### External Contacts

- **Cloud Provider Support**: [Provider Number]
- **Database Vendor**: [Vendor Number]
- **Security Team**: security@wateroptimizer.org

---

## Appendix

### Useful Commands

```bash
# Port forward for debugging
kubectl port-forward svc/auth-service 8086:8086 -n water-optimizer

# Execute command in pod
kubectl exec -it deployment/auth-service -n water-optimizer -- /bin/bash

# Copy file from pod
kubectl cp water-optimizer/auth-service-xxx:/app/logs/app.log ./app.log

# Watch events
kubectl get events -n water-optimizer --watch

# Describe for troubleshooting
kubectl describe pod auth-service-xxx -n water-optimizer
```

### Monitoring URLs

- **Grafana**: https://grafana.wateroptimizer.org
- **Prometheus**: https://prometheus.wateroptimizer.org
- **Kibana**: https://kibana.wateroptimizer.org (logs)
- **Kubernetes Dashboard**: https://dashboard.k8s.wateroptimizer.org

---

**Last Updated**: January 2024
**Version**: 1.0.0
**Owner**: DevOps Team

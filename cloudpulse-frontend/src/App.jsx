import { useEffect, useState } from "react";
import "./App.css";

const API_URL = "/api/dashboard";

function App() {
  const [dashboard, setDashboard] = useState({
    instances: [],
    cpuMetrics: [],
    securityGroups: [],
    recommendations: [],
  });

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    fetch(API_URL)
      .then((res) => {
        if (!res.ok) {
          throw new Error("대시보드 API 호출 실패");
        }
        return res.json();
      })
      .then((data) => {
        setDashboard({
          instances: data.instances || [],
          cpuMetrics: data.cpuMetrics || [],
          securityGroups: data.securityGroups || [],
          recommendations: data.recommendations || [],
        });
      })
      .catch((err) => {
        setError(err.message);
      })
      .finally(() => {
        setLoading(false);
      });
  }, []);

  const instances = dashboard.instances;
  const cpuMetrics = dashboard.cpuMetrics;
  const securityGroups = dashboard.securityGroups;
  const recommendations = dashboard.recommendations;

  const totalEc2 = instances.length;
  const runningCount = instances.filter((i) => i.state === "running").length;
  const stoppedCount = instances.filter((i) => i.state === "stopped").length;

  const securityRiskCount = securityGroups.reduce(
    (sum, sg) => sum + (sg.risks?.length || 0),
    0
  );

  const lowCpuCount = cpuMetrics.filter((cpu) => cpu.status === "저사용").length;
  const highCpuCount = cpuMetrics.filter(
    (cpu) => cpu.status === "과부하 위험"
  ).length;

  const recommendationCount = recommendations.length;

  if (loading) {
    return <div className="loading">CloudPulse 데이터를 불러오는 중...</div>;
  }

  if (error) {
    return <div className="error">오류 발생: {error}</div>;
  }

  return (
    <div className="app">
      <header className="header">
        <h1>CloudPulse Dashboard</h1>
        <p>AWS 리소스 상태, 모니터링, 보안 점검 및 개선 권고를 한눈에 확인합니다.</p>
      </header>

      <section className="summary-grid">
        <SummaryCard title="전체 EC2 수" value={totalEc2} />
        <SummaryCard title="Running 인스턴스" value={runningCount} />
        <SummaryCard title="Stopped 인스턴스" value={stoppedCount} />
        <SummaryCard title="보안 위험 건수" value={securityRiskCount} danger />
        <SummaryCard title="저사용 인스턴스" value={lowCpuCount} warning />
        <SummaryCard title="과부하 위험 인스턴스" value={highCpuCount} danger />
        <SummaryCard title="개선 권고 건수" value={recommendationCount} />
      </section>

      <main className="dashboard-grid">
        <section className="panel">
          <h2>EC2 상태 조회</h2>

          {instances.length === 0 ? (
            <EmptyMessage message="조회된 EC2 인스턴스가 없습니다." />
          ) : (
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>이름</th>
                    <th>인스턴스 ID</th>
                    <th>상태</th>
                    <th>타입</th>
                    <th>리전</th>
                    <th>Public IP</th>
                  </tr>
                </thead>
                <tbody>
                  {instances.map((instance) => (
                    <tr key={instance.instanceId}>
                      <td>{instance.instanceName || "-"}</td>
                      <td>{instance.instanceId}</td>
                      <td>
                        <StatusBadge status={instance.state} />
                      </td>
                      <td>{instance.instanceType}</td>
                      <td>{instance.region}</td>
                      <td>{instance.publicIp || "-"}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>

        <section className="panel">
          <h2>CloudWatch 모니터링</h2>

          {cpuMetrics.length === 0 ? (
            <EmptyMessage message="조회된 CPU 메트릭이 없습니다." />
          ) : (
            <div className="card-list">
              {cpuMetrics.map((cpu) => (
                <div className="data-card" key={cpu.instanceId}>
                  <div className="data-card-title">
                    {cpu.instanceName || cpu.instanceId}
                    <StatusBadge status={cpu.status} />
                  </div>

                  <div className="data-row">
                    <span>인스턴스 ID</span>
                    <strong>{cpu.instanceId}</strong>
                  </div>
                  <div className="data-row">
                    <span>리전</span>
                    <strong>{cpu.region}</strong>
                  </div>
                  <div className="data-row">
                    <span>평균 CPU</span>
                    <strong>{cpu.averageCpu}%</strong>
                  </div>
                  <div className="data-row">
                    <span>최대 CPU</span>
                    <strong>{cpu.maxCpu}%</strong>
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>

        <section className="panel">
          <h2>보안 점검</h2>

          {securityGroups.length === 0 ? (
            <EmptyMessage message="조회된 Security Group이 없습니다." />
          ) : (
            <div className="card-list">
              {securityGroups.map((sg) => (
                <div className="data-card" key={sg.securityGroupId}>
                  <div className="data-card-title">
                    {sg.securityGroupName}
                    <SeverityBadge severity={getHighestSeverity(sg.risks)} />
                  </div>

                  <div className="data-row">
                    <span>Security Group ID</span>
                    <strong>{sg.securityGroupId}</strong>
                  </div>
                  <div className="data-row">
                    <span>리전</span>
                    <strong>{sg.region}</strong>
                  </div>
                  <div className="data-row">
                    <span>EC2 연결 여부</span>
                    <strong>{sg.attachedToEc2 ? "연결됨" : "미연결"}</strong>
                  </div>
                  <div className="data-row">
                    <span>Public IP 노출</span>
                    <strong>{sg.publicIpExposed ? "있음" : "없음"}</strong>
                  </div>

                  {sg.risks?.length > 0 && (
                    <ul className="risk-list">
                      {sg.risks.map((risk, index) => (
                        <li key={index}>
                          <strong>[{risk.severity}]</strong> {risk.message}
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              ))}
            </div>
          )}
        </section>

        <section className="panel">
          <h2>개선 제안</h2>

          {recommendations.length === 0 ? (
            <EmptyMessage message="현재 표시할 개선 권고가 없습니다." />
          ) : (
            <div className="card-list">
              {recommendations.map((rec, index) => (
                <div className="recommend-card" key={`${rec.targetId}-${index}`}>
                  <div className="recommend-header">
                    <span className="category">{rec.category}</span>
                    <SeverityBadge severity={rec.severity} />
                  </div>

                  <h3>{rec.title}</h3>

                  <p className="target">
                    {rec.targetName} / {rec.targetId}
                  </p>

                  <p className="current-status">{rec.currentStatus}</p>

                  <div className="recommend-box">
                    {rec.recommendation}
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>
      </main>
    </div>
  );
}

function SummaryCard({ title, value, danger, warning }) {
  let className = "summary-card";

  if (danger) className += " danger";
  if (warning) className += " warning";

  return (
    <div className={className}>
      <span>{title}</span>
      <strong>{value}</strong>
    </div>
  );
}

function StatusBadge({ status }) {
  let className = "badge";

  if (status === "running" || status === "정상") {
    className += " success";
  } else if (status === "stopped" || status === "저사용") {
    className += " warning";
  } else if (status === "과부하 위험") {
    className += " danger";
  } else {
    className += " neutral";
  }

  return <span className={className}>{status}</span>;
}

function SeverityBadge({ severity }) {
  let className = "badge";

  if (severity === "HIGH") className += " danger";
  else if (severity === "MEDIUM") className += " warning";
  else if (severity === "LOW") className += " info";
  else className += " neutral";

  return <span className={className}>{severity || "NONE"}</span>;
}

function EmptyMessage({ message }) {
  return <div className="empty">{message}</div>;
}

function getHighestSeverity(risks = []) {
  if (risks.some((risk) => risk.severity === "HIGH")) return "HIGH";
  if (risks.some((risk) => risk.severity === "MEDIUM")) return "MEDIUM";
  if (risks.some((risk) => risk.severity === "LOW")) return "LOW";
  return "NONE";
}

export default App;
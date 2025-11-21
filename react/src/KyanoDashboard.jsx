import React, { useState, useEffect, useRef } from 'react';

// Historical and Optimized Data
const historicalData = [
  { date: '2024-09-10', otif: 94.5, status: 'normal', event: 'Baseline operations', statusColor: '#10b981', orders: 1847, onTime: 1745, delayed: 102, risk: 'Low' },
  { date: '2024-09-11', otif: 94.2, status: 'normal', event: 'Baseline operations', statusColor: '#10b981', orders: 1893, onTime: 1783, delayed: 110, risk: 'Low' },
  { date: '2024-09-12', otif: 93.8, status: 'normal', event: 'Baseline operations', statusColor: '#10b981', orders: 1826, onTime: 1713, delayed: 113, risk: 'Low' },
  { date: '2024-09-13', otif: 92.1, status: 'warning', event: 'Minor delay in Nordic region', statusColor: '#f59e0b', orders: 1954, onTime: 1800, delayed: 154, risk: 'Medium' },
  { date: '2024-09-14', otif: 89.5, status: 'alert', event: 'Demand spike detected: Norway +35%', statusColor: '#f97316', orders: 2134, onTime: 1910, delayed: 224, risk: 'High' },
  { date: '2024-09-15', otif: 87.2, status: 'critical', event: 'Stockout risk: 1.2 days', statusColor: '#ef4444', orders: 2298, onTime: 2004, delayed: 294, risk: 'Critical' },
  { date: '2024-09-16', otif: 86.8, status: 'critical', event: 'Critical: OTIF dropping', statusColor: '#ef4444', orders: 2367, onTime: 2054, delayed: 313, risk: 'Critical' },
  { date: '2024-09-17', otif: 88.5, status: 'recovering', event: 'DECISION: Transfer 13K cases approved', statusColor: '#3b82f6', orders: 2189, onTime: 1937, delayed: 252, risk: 'Medium' },
  { date: '2024-09-18', otif: 92.3, status: 'recovering', event: 'Transfer in transit', statusColor: '#3b82f6', orders: 2045, onTime: 1888, delayed: 157, risk: 'Medium' },
  { date: '2024-09-19', otif: 95.8, status: 'recovered', event: 'Transfer delivered', statusColor: '#10b981', orders: 1967, onTime: 1884, delayed: 83, risk: 'Low' },
  { date: '2024-09-20', otif: 98.0, status: 'excellent', event: 'OTIF restored, €26K saved', statusColor: '#059669', orders: 1823, onTime: 1787, delayed: 36, risk: 'Very Low' },
  { date: '2024-09-21', otif: 97.2, status: 'excellent', event: 'Stabilized operations', statusColor: '#059669', orders: 1891, onTime: 1838, delayed: 53, risk: 'Very Low' },
  { date: '2024-09-22', otif: 96.5, status: 'normal', event: 'Normal operations', statusColor: '#10b981', orders: 1856, onTime: 1791, delayed: 65, risk: 'Low' }
];

const optimizedData = [
  { date: '2024-09-10', otif: 94.5, status: 'normal', event: 'Baseline operations', statusColor: '#10b981', orders: 1847, onTime: 1745, delayed: 102, risk: 'Low' },
  { date: '2024-09-11', otif: 94.2, status: 'normal', event: 'Baseline operations', statusColor: '#10b981', orders: 1893, onTime: 1783, delayed: 110, risk: 'Low' },
  { date: '2024-09-12', otif: 93.8, status: 'normal', event: 'Baseline operations', statusColor: '#10b981', orders: 1826, onTime: 1713, delayed: 113, risk: 'Low' },
  { date: '2024-09-13', otif: 92.1, status: 'warning', event: 'Minor delay detected', statusColor: '#f59e0b', orders: 1954, onTime: 1800, delayed: 154, risk: 'Medium' },
  { date: '2024-09-14', otif: 89.5, status: 'alert', event: 'Early warning triggered', statusColor: '#f97316', orders: 2134, onTime: 1910, delayed: 224, risk: 'High' },
  { date: '2024-09-15', otif: 90.2, status: 'recovering', event: 'OPTIMIZED: Proactive transfer', statusColor: '#3b82f6', orders: 2298, onTime: 2073, delayed: 225, risk: 'Medium' },
  { date: '2024-09-16', otif: 93.5, status: 'recovering', event: 'Transfer in progress', statusColor: '#3b82f6', orders: 2367, onTime: 2213, delayed: 154, risk: 'Low' },
  { date: '2024-09-17', otif: 96.8, status: 'excellent', event: 'Crisis averted', statusColor: '#059669', orders: 2189, onTime: 2119, delayed: 70, risk: 'Very Low' },
  { date: '2024-09-18', otif: 97.5, status: 'excellent', event: 'High performance', statusColor: '#059669', orders: 2045, onTime: 1994, delayed: 51, risk: 'Very Low' },
  { date: '2024-09-19', otif: 97.1, status: 'excellent', event: 'Operations normalized', statusColor: '#059669', orders: 1967, onTime: 1910, delayed: 57, risk: 'Very Low' },
  { date: '2024-09-20', otif: 98.2, status: 'excellent', event: 'Optimal performance', statusColor: '#059669', orders: 1823, onTime: 1790, delayed: 33, risk: 'Very Low' },
  { date: '2024-09-21', otif: 97.6, status: 'excellent', event: 'Sustained excellence', statusColor: '#059669', orders: 1891, onTime: 1846, delayed: 45, risk: 'Very Low' },
  { date: '2024-09-22', otif: 97.2, status: 'excellent', event: 'Normal operations', statusColor: '#059669', orders: 1856, onTime: 1804, delayed: 52, risk: 'Low' }
];

const teamMembers = [
  { id: '1', name: 'Sarah Chen', role: 'Supply Chain Director', color: '#3b82f6' },
  { id: '2', name: 'Marcus Johnson', role: 'Finance Manager', color: '#10b981' },
  { id: '3', name: 'Emma Larsen', role: 'Logistics Coordinator', color: '#f59e0b' },
  { id: '4', name: 'David Park', role: 'Demand Planner', color: '#8b5cf6' },
  { id: '5', name: 'Lisa Anderson', role: 'Customer Success', color: '#ec4899' }
];

// Chart Component
const OTIFChart = ({ data, currentIndex, showOptimized = false, onHover }) => {
  const canvasRef = useRef(null);
  const [hoveredPoint, setHoveredPoint] = useState(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    const width = canvas.width;
    const height = canvas.height;

    ctx.clearRect(0, 0, width, height);

    // Draw grid
    ctx.strokeStyle = '#334155';
    ctx.lineWidth = 1;
    for (let i = 0; i <= 4; i++) {
      const y = (height / 4) * i;
      ctx.beginPath();
      ctx.moveTo(0, y);
      ctx.lineTo(width, y);
      ctx.stroke();
    }

    // Y-axis labels
    ctx.fillStyle = '#64748b';
    ctx.font = '11px sans-serif';
    ctx.textAlign = 'right';
    for (let i = 0; i <= 4; i++) {
      const value = 100 - (i * 5);
      const y = (height / 4) * i + 5;
      ctx.fillText(value + '%', width - 10, y);
    }

    const visibleData = data.slice(0, currentIndex + 1);

    if (showOptimized) {
      // Draw actual line faded
      const actualVisible = historicalData.slice(0, currentIndex + 1);
      ctx.strokeStyle = 'rgba(100, 116, 139, 0.3)';
      ctx.lineWidth = 2;
      ctx.setLineDash([5, 5]);
      ctx.beginPath();
      actualVisible.forEach((d, i) => {
        const x = (i / (historicalData.length - 1)) * (width - 20);
        const y = height - ((d.otif - 80) / 20 * height);
        if (i === 0) ctx.moveTo(x, y);
        else ctx.lineTo(x, y);
      });
      ctx.stroke();
      ctx.setLineDash([]);
    }

    // Draw main line
    ctx.strokeStyle = showOptimized ? '#10b981' : '#2563eb';
    ctx.lineWidth = 3;
    ctx.beginPath();

    visibleData.forEach((d, i) => {
      const x = (i / (data.length - 1)) * (width - 20);
      const y = height - ((d.otif - 80) / 20 * height);
      d._x = x;
      d._y = y;
      if (i === 0) ctx.moveTo(x, y);
      else ctx.lineTo(x, y);
    });
    ctx.stroke();

    // Draw points
    visibleData.forEach((d) => {
      ctx.fillStyle = d.statusColor;
      ctx.beginPath();
      ctx.arc(d._x, d._y, 6, 0, Math.PI * 2);
      ctx.fill();
    });
  }, [data, currentIndex, showOptimized]);

  const handleMouseMove = (e) => {
    const canvas = canvasRef.current;
    const rect = canvas.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;

    const visibleData = data.slice(0, currentIndex + 1);
    let found = null;

    for (let d of visibleData) {
      if (d._x && d._y) {
        const distance = Math.sqrt(Math.pow(x - d._x, 2) + Math.pow(y - d._y, 2));
        if (distance < 15) {
          found = { ...d, screenX: e.clientX, screenY: e.clientY };
          break;
        }
      }
    }

    setHoveredPoint(found);
  };

  return (
    <div className="relative">
      <canvas
        ref={canvasRef}
        width={800}
        height={300}
        onMouseMove={handleMouseMove}
        onMouseLeave={() => setHoveredPoint(null)}
        className="cursor-crosshair"
      />
      {hoveredPoint && (
        <div
          className="absolute bg-slate-800 border-2 border-blue-500 rounded-lg p-3 pointer-events-none z-50 min-w-[200px] shadow-xl"
          style={{ left: hoveredPoint.screenX - rect?.left + 15, top: hoveredPoint.screenY - rect?.top - 10 }}
        >
          <div className="font-bold text-blue-400 mb-2 text-sm">{hoveredPoint.date}</div>
          <div className="text-xs space-y-1">
            <div className="flex justify-between">
              <span className="text-slate-400">OTIF:</span>
              <span className="font-bold" style={{ color: hoveredPoint.statusColor }}>{hoveredPoint.otif}%</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-400">Orders:</span>
              <span className="font-bold">{hoveredPoint.orders}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-400">On Time:</span>
              <span className="font-bold text-green-500">{hoveredPoint.onTime}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-400">Delayed:</span>
              <span className="font-bold text-red-500">{hoveredPoint.delayed}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-400">Risk:</span>
              <span className="font-bold">{hoveredPoint.risk}</span>
            </div>
            <div className="mt-2 pt-2 border-t border-slate-600 text-slate-400 text-xs">
              {hoveredPoint.event}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

// Main Dashboard Component
export default function KyanoDashboard() {
  const [activeTab, setActiveTab] = useState('overview');
  const [currentIndex, setCurrentIndex] = useState(12);
  const [isPlaying, setIsPlaying] = useState(false);
  const [showConfidenceModal, setShowConfidenceModal] = useState(false);
  const [roomParticipants, setRoomParticipants] = useState([]);
  const [activityLog, setActivityLog] = useState([
    { user: 'System', time: '2 mins ago', action: 'Decision room created for Norway supply disruption' }
  ]);
  const [votingStatus, setVotingStatus] = useState(null);
  const [draggedMember, setDraggedMember] = useState(null);

  // What-if state
  const [demand, setDemand] = useState(0);
  const [leadtime, setLeadtime] = useState(0);
  const [capacity, setCapacity] = useState(0);

  const currentData = historicalData[currentIndex];

  const calculateProjectedOTIF = () => {
    const base = 94.2;
    const demandImpact = demand * -0.15;
    const leadtimeImpact = leadtime * -0.25;
    const capacityImpact = capacity * 0.18;
    return Math.max(75, Math.min(100, base + demandImpact + leadtimeImpact + capacityImpact));
  };

  const projectedOTIF = calculateProjectedOTIF();

  const playTimeline = () => {
    if (isPlaying) return;
    setIsPlaying(true);
    let index = 0;
    const interval = setInterval(() => {
      if (index >= historicalData.length - 1) {
        clearInterval(interval);
        setIsPlaying(false);
        return;
      }
      setCurrentIndex(index);
      index++;
    }, 800);
  };

  const addParticipant = (member) => {
    if (roomParticipants.find(p => p.id === member.id)) return;
    setRoomParticipants([...roomParticipants, member]);
    setActivityLog([
      { user: 'System', time: 'Just now', action: `${member.name} joined the decision room` },
      ...activityLog
    ]);
  };

  const removeParticipant = (id) => {
    const member = roomParticipants.find(p => p.id === id);
    setRoomParticipants(roomParticipants.filter(p => p.id !== id));
    if (member) {
      setActivityLog([
        { user: 'System', time: 'Just now', action: `${member.name} left the decision room` },
        ...activityLog
      ]);
    }
  };

  const startVoting = () => {
    if (roomParticipants.length === 0) {
      alert('Add team members first!');
      return;
    }
    setVotingStatus('inProgress');
    setActivityLog([
      { user: 'System', time: 'Just now', action: 'Voting initiated' },
      ...activityLog
    ]);

    roomParticipants.forEach((p, i) => {
      setTimeout(() => {
        setActivityLog(prev => [
          { user: 'System', time: `${i + 1}s ago`, action: `${p.name} voted for Emergency Transfer` },
          ...prev
        ]);
      }, (i + 1) * 1500);
    });

    setTimeout(() => {
      setVotingStatus('complete');
    }, roomParticipants.length * 1500 + 1000);
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 to-slate-800 text-slate-100">
      {/* Header */}
      <div className="bg-slate-800 border-b-2 border-slate-700 px-6 py-4">
        <div className="flex justify-between items-center">
          <div>
            <h1 className="text-2xl font-bold">Kyano Decision Intelligence</h1>
            <p className="text-sm text-slate-400 mt-1">Supply Chain OTIF Performance & Decision Support</p>
          </div>
          <div className="text-right">
            <div className="text-3xl font-extrabold" style={{ color: currentData.statusColor }}>
              {currentData.otif.toFixed(1)}%
            </div>
            <div className="text-xs text-slate-400 uppercase tracking-wide">Current OTIF</div>
          </div>
        </div>
      </div>

      {/* Time Navigation */}
      <div className="bg-slate-800 border-b border-slate-700 px-6 py-4">
        <div className="flex justify-between items-center mb-3 text-sm text-slate-400">
          <div>
            <strong className="text-slate-100">{currentData.date}</strong> | 
            Status: <strong style={{ color: currentData.statusColor }}> {currentData.status.charAt(0).toUpperCase() + currentData.status.slice(1)}</strong> | 
            <span className="ml-2">{currentData.event}</span>
          </div>
          <div className="flex gap-2">
            <button 
              onClick={playTimeline}
              disabled={isPlaying}
              className="px-3 py-1.5 bg-blue-600 hover:bg-blue-700 rounded text-xs font-semibold disabled:bg-slate-600 transition-all"
            >
              {isPlaying ? '⏸ Playing...' : '▶ Play Timeline'}
            </button>
            <button 
              onClick={() => setCurrentIndex(7)}
              className="px-3 py-1.5 bg-green-600 hover:bg-green-700 rounded text-xs font-semibold transition-all"
            >
              ⚡ Jump to Decision
            </button>
          </div>
        </div>
        <input
          type="range"
          min="0"
          max="12"
          value={currentIndex}
          onChange={(e) => setCurrentIndex(parseInt(e.target.value))}
          className="w-full h-2 bg-slate-700 rounded-lg appearance-none cursor-pointer"
        />
        <div className="flex justify-between text-xs text-slate-500 mt-1">
          <span>Sep 10</span>
          <span>Sep 16 (Decision Point)</span>
          <span>Sep 22 (Today)</span>
        </div>
      </div>

      {/* Tabs */}
      <div className="bg-slate-800 border-b border-slate-700 px-6 flex gap-1">
        {[
          { id: 'overview', label: 'Overview' },
          { id: 'decision-history', label: 'Decision History' },
          { id: 'decision-room', label: 'Decision Room' },
          { id: 'what-if', label: 'What-If Scenarios' },
          { id: 'retailer-collab', label: 'Retailer Collaboration' },
          { id: 'similar-cases', label: 'Similar Cases' }
        ].map(tab => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id)}
            className={`px-5 py-3 text-sm font-semibold border-b-2 transition-all ${
              activeTab === tab.id
                ? 'bg-slate-700 border-blue-500 text-slate-100'
                : 'border-transparent text-slate-400 hover:text-slate-200'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Content */}
      <div className="p-6 overflow-auto" style={{ height: 'calc(100vh - 280px)' }}>
        {/* Overview Tab */}
        {activeTab === 'overview' && (
          <div className="grid grid-cols-3 gap-5 h-full">
            <div className="col-span-2 space-y-5">
              <div className="bg-slate-800 rounded-lg p-5 border border-slate-700">
                <h3 className="text-lg font-semibold mb-4">OTIF Performance Timeline - Actual</h3>
                <OTIFChart data={historicalData} currentIndex={currentIndex} />
              </div>

              <div className="bg-slate-800 rounded-lg p-5 border border-slate-700">
                <h3 className="text-lg font-semibold mb-4">
                  OTIF Performance Timeline - Optimized Scenario
                  <span 
                    onClick={() => alert('Optimized Scenario:\n\nShows what would happen if decision was made 2 days earlier.\n\nBenefits:\n• OTIF never drops below 90%\n• Additional €8,200 saved\n• 4.2% better satisfaction')}
                    className="ml-2 text-xs bg-slate-700 px-2 py-1 rounded cursor-help hover:bg-blue-600 transition-all"
                  >
                    ℹ️ What if?
                  </span>
                </h3>
                <OTIFChart data={optimizedData} currentIndex={currentIndex} showOptimized={true} />
                <div className="mt-4 p-3 bg-green-900/20 border-l-4 border-green-500 rounded">
                  <strong className="text-green-500">Optimization Impact:</strong> Earlier intervention on Sep 15 would have prevented OTIF from dropping below 90%, saving an additional €8,200 and improving customer satisfaction by 4.2%.
                </div>
              </div>
            </div>

            <div className="space-y-4">
              <div className="bg-slate-800 rounded-lg p-5 border border-slate-700">
                <div className="text-xs text-slate-400 uppercase mb-2">Orders at Risk</div>
                <div className="text-4xl font-extrabold">
                  {currentData.status === 'critical' ? '47' : currentData.status === 'alert' ? '23' : '8'}
                </div>
                <div className="text-xs text-slate-500 mt-1">Within next 48 hours</div>
              </div>

              <div className="bg-slate-800 rounded-lg p-5 border border-slate-700">
                <div className="text-xs text-slate-400 uppercase mb-2">Value at Stake</div>
                <div className="text-4xl font-extrabold">
                  {currentData.status === 'critical' ? '€31K' : currentData.status === 'alert' ? '€18K' : '€5K'}
                </div>
                <div className="text-xs text-slate-500 mt-1">Potential revenue impact</div>
              </div>

              {currentIndex >= 9 && (
                <div className="bg-slate-800 rounded-lg p-5 border border-slate-700">
                  <div className="text-xs text-slate-400 uppercase mb-2">Decision Value Created</div>
                  <div className="text-4xl font-extrabold text-green-500">€26,400</div>
                  <div className="text-xs text-slate-500 mt-1">Emergency transfer ROI: 5.2x</div>
                </div>
              )}
            </div>
          </div>
        )}

        {/* Decision History Tab */}
        {activeTab === 'decision-history' && (
          <div className="space-y-6">
            <div className="bg-gradient-to-r from-slate-800 to-slate-900 rounded-lg p-5 border border-slate-700">
              <h3 className="text-lg font-semibold mb-2">Critical Decision: Norway Demand Spike</h3>
              <div className="text-sm text-slate-400 mb-4">September 17, 2024 | Decision made in 6.2 hours | AI-Assisted</div>
              <div className="bg-red-900/20 border-l-4 border-red-500 p-4 rounded">
                <strong className="text-red-400">Situation:</strong> Unexpected demand spike in Norway (+35%), stockout risk in 1.2 days, OTIF dropping from 94.5% to 86.8%
              </div>
            </div>

            <div className="grid grid-cols-3 gap-4">
              {[
                { title: 'Emergency Transfer', badge: '✓ Chosen', badgeColor: 'bg-green-600', cost: '€5,080', time: '36h', otif: '+11.2%', roi: '5.2x', roiColor: 'text-green-500', why: 'Fastest response with highest ROI. Leverages Denmark warehouse (240km). Risk: 12% (Low)', bgColor: 'from-green-700 to-green-800' },
                { title: 'Expedited Production', badge: 'Alternative', badgeColor: 'bg-slate-600', cost: '€8,920', time: '72h', otif: '+8.5%', roi: '2.9x', roiColor: 'text-slate-300', why: 'Higher cost, longer lead time. Risk: 28% (Medium). Production capacity constraint.', bgColor: 'from-slate-700 to-slate-800' },
                { title: 'Do Nothing', badge: 'Rejected', badgeColor: 'bg-slate-600', cost: '€0', time: '—', otif: '-7.8%', roi: '-€31K', roiColor: 'text-red-500', why: 'Zero cost but severe consequences. Lost revenue + penalties + damaged relationships. Risk: 89% (Critical)', bgColor: 'from-slate-700 to-slate-800' }
              ].map((scenario, idx) => (
                <div key={idx} className={`bg-gradient-to-br ${scenario.bgColor} rounded-lg p-5 border border-slate-600 relative`}>
                  <span className={`absolute top-3 right-3 ${scenario.badgeColor} px-2 py-1 rounded text-xs font-bold`}>
                    {scenario.badge}
                  </span>
                  <h4 className="text-lg font-bold mb-4 pr-20">{scenario.title}</h4>
                  <div className="grid grid-cols-2 gap-3 mb-3">
                    <div>
                      <div className="text-xs text-slate-400">COST</div>
                      <div className="text-2xl font-bold">{scenario.cost}</div>
                    </div>
                    <div>
                      <div className="text-xs text-slate-400">TIME</div>
                      <div className="text-2xl font-bold">{scenario.time}</div>
                    </div>
                    <div>
                      <div className="text-xs text-slate-400">OTIF IMPACT</div>
                      <div className="text-2xl font-bold">{scenario.otif}</div>
                    </div>
                    <div>
                      <div className="text-xs text-slate-400">ROI</div>
                      <div className={`text-2xl font-bold ${scenario.roiColor}`}>{scenario.roi}</div>
                    </div>
                  </div>
                  <div className="bg-black/20 rounded p-3 text-xs leading-relaxed">
                    <strong>Why:</strong> {scenario.why}
                  </div>
                </div>
              ))}
            </div>

            {/* Scenario Comparison Chart */}
            <div className="bg-slate-900 rounded-lg p-6 border border-slate-700">
              <h4 className="text-center text-lg font-semibold mb-5">📊 Scenario Comparison Analysis - Why Emergency Transfer Won</h4>
              <div className="space-y-4">
                {[
                  { label: 'Cost Efficiency', bars: [{ text: 'Emergency: €5,080', width: '85%', winner: true }, { text: 'Expedited: €8,920', width: '50%' }, { text: 'Wait: €0', width: '95%' }] },
                  { label: 'ROI Multiple', bars: [{ text: '5.2x', width: '100%', winner: true }, { text: '2.9x', width: '56%' }, { text: '—', width: '0%' }] },
                  { label: 'Speed', bars: [{ text: '36 hours', width: '100%', winner: true }, { text: '72 hours', width: '50%' }, { text: '∞', width: '20%' }] },
                  { label: 'Risk Level', bars: [{ text: '12% Low', width: '12%', winner: true, color: 'from-green-600 to-green-700' }, { text: '28% Med', width: '28%', color: 'bg-yellow-600' }, { text: '89% Critical', width: '89%', color: 'bg-red-600' }] },
                  { label: 'OTIF Improvement', bars: [{ text: '+11.2%', width: '100%', winner: true }, { text: '+8.5%', width: '76%' }, { text: '-7.8%', width: '30%', color: 'bg-red-600' }] },
                  { label: 'Value Created', bars: [{ text: '€26,400', width: '100%', winner: true }, { text: '€16,850', width: '62%' }, { text: '-€31,000', width: '15%', color: 'bg-red-600' }] }
                ].map((row, idx) => (
                  <div key={idx} className="grid grid-cols-12 items-center gap-3">
                    <div className="col-span-2 text-sm font-semibold text-slate-400">{row.label}</div>
                    <div className="col-span-10 flex gap-2">
                      {row.bars.map((bar, bidx) => (
                        <div
                          key={bidx}
                          style={{ width: bar.width }}
                          className={`h-8 rounded flex items-center justify-center text-xs font-bold cursor-pointer hover:scale-105 transition-transform ${
                            bar.winner ? 'bg-gradient-to-r from-green-600 to-green-700 border-2 border-green-400 shadow-lg shadow-green-500/50' :
                            bar.color || 'bg-slate-600'
                          }`}
                        >
                          {bar.text}
                        </div>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
              <div className="mt-6 p-4 bg-green-900/20 border-l-4 border-green-500 rounded">
                <strong className="text-green-500">Decision Rationale:</strong> Emergency Transfer achieved optimal balance: lowest risk (12%), fastest resolution (36h), and highest ROI (5.2x). The proximity advantage of Denmark warehouse (240km) provided decisive logistics efficiency.
              </div>
            </div>
          </div>
        )}

        {/* Decision Room Tab */}
        {activeTab === 'decision-room' && (
          <div>
            <div className="bg-slate-800 rounded-lg p-5 border border-slate-700 mb-5">
              <h3 className="text-lg font-semibold mb-2">🎯 Active Decision Room: Norway Supply Disruption</h3>
              <p className="text-sm text-slate-400">Collaborative decision-making space for real-time stakeholder alignment</p>
            </div>

            <div className="grid grid-cols-4 gap-5" style={{ height: 'calc(100vh - 450px)' }}>
              <div className="bg-slate-800 rounded-lg p-5 border border-slate-700 overflow-y-auto">
                <h4 className="text-sm font-semibold text-slate-400 mb-3">Available Team Members</h4>
                <div className="space-y-2">
                  {teamMembers.map(member => (
                    <div
                      key={member.id}
                      draggable
                      onDragStart={(e) => {
                        setDraggedMember(member);
                        e.currentTarget.style.opacity = '0.5';
                      }}
                      onDragEnd={(e) => {
                        e.currentTarget.style.opacity = '1';
                      }}
                      className={`flex items-center gap-2 p-2 rounded cursor-move border-2 transition-all ${
                        roomParticipants.find(p => p.id === member.id)
                          ? 'bg-blue-900 border-blue-500'
                          : 'bg-slate-900 border-transparent hover:border-blue-500'
                      }`}
                    >
                      <div 
                        className="w-9 h-9 rounded-full flex items-center justify-center font-bold text-sm flex-shrink-0"
                        style={{ background: `linear-gradient(135deg, ${member.color}, ${member.color}dd)` }}
                      >
                        {member.name.split(' ').map(n => n[0]).join('')}
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="font-semibold text-sm truncate">{member.name}</div>
                        <div className="text-xs text-slate-400 truncate">{member.role}</div>
                      </div>
                    </div>
                  ))}
                </div>
                <div className="mt-4 p-3 bg-blue-900/20 rounded text-xs text-slate-400">
                  💡 Drag team members into the decision room to start collaborating
                </div>
              </div>

              <div className="col-span-3 space-y-5 overflow-y-auto">
                <div className="bg-slate-800 rounded-lg p-5 border border-slate-700">
                  <div
                    onDragOver={(e) => {
                      e.preventDefault();
                      e.currentTarget.classList.add('border-blue-500', 'bg-blue-900/10');
                    }}
                    onDragLeave={(e) => {
                      e.currentTarget.classList.remove('border-blue-500', 'bg-blue-900/10');
                    }}
                    onDrop={(e) => {
                      e.preventDefault();
                      e.currentTarget.classList.remove('border-blue-500', 'bg-blue-900/10');
                      if (draggedMember) {
                        addParticipant(draggedMember);
                        setDraggedMember(null);
                      }
                    }}
                    className="border-2 border-dashed border-slate-600 rounded-lg p-5 min-h-[120px] transition-all"
                  >
                    <h4 className="text-sm font-semibold text-slate-400 mb-3">👥 Decision Room Participants</h4>
                    {roomParticipants.length === 0 ? (
                      <div className="text-center text-slate-500 text-sm py-8">
                        Drag team members here to add them to the decision room
                      </div>
                    ) : (
                      <div className="flex flex-wrap gap-3">
                        {roomParticipants.map(member => (
                          <div key={member.id} className="flex items-center gap-2 px-3 py-2 bg-slate-700 rounded border border-blue-500">
                            <div 
                              className="w-8 h-8 rounded-full flex items-center justify-center font-bold text-xs"
                              style={{ background: `linear-gradient(135deg, ${member.color}, ${member.color}dd)` }}
                            >
                              {member.name.split(' ').map(n => n[0]).join('')}
                            </div>
                            <div>
                              <div className="font-semibold text-sm">{member.name}</div>
                              <div className="text-xs text-slate-400">{member.role}</div>
                            </div>
                            <button
                              onClick={() => removeParticipant(member.id)}
                              className="ml-2 px-2 py-1 bg-slate-600 hover:bg-red-600 rounded text-xs font-bold transition-colors"
                            >
                              ×
                            </button>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                </div>

                <div className="bg-slate-800 rounded-lg p-5 border border-slate-700 max-h-80 overflow-y-auto">
                  <h4 className="text-sm font-semibold mb-3">📝 Decision Activity Timeline</h4>
                  <div className="space-y-3">
                    {activityLog.map((log, idx) => (
                      <div key={idx} className="relative pl-8 pb-3 border-l-2 border-slate-600 last:border-transparent ml-2">
                        <div className="absolute left-[-9px] top-0 w-4 h-4 rounded-full bg-blue-600 border-2 border-slate-900"></div>
                        <div className="bg-slate-900 rounded p-3">
                          <div className="flex justify-between mb-1">
                            <span className="font-semibold text-sm">{log.user}</span>
                            <span className="text-xs text-slate-400">{log.time}</span>
                          </div>
                          <div className="text-sm text-slate-400">{log.action}</div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>

                <div className="bg-slate-800 rounded-lg p-5 border border-slate-700">
                  <h4 className="text-sm font-semibold mb-3">⚡ Collaborative Actions</h4>
                  <div className="flex gap-3 flex-wrap">
                    <button onClick={startVoting} className="flex-1 min-w-[150px] px-4 py-2 bg-blue-600 hover:bg-blue-700 rounded font-semibold text-sm transition-all">
                      🗳️ Start Vote
                    </button>
                    <button 
                      onClick={() => {
                        if (roomParticipants.length === 0) {
                          alert('Add team members first!');
                          return;
                        }
                        setActivityLog([{ user: 'System', time: 'Just now', action: 'Approval requests sent' }, ...activityLog]);
                        alert('Approval requests sent to:\n• Finance Manager\n• Supply Chain Director\n• Logistics Coordinator');
                      }}
                      className="flex-1 min-w-[150px] px-4 py-2 bg-yellow-600 hover:bg-yellow-700 rounded font-semibold text-sm transition-all"
                    >
                      ✓ Request Approvals
                    </button>
                    <button 
                      onClick={() => {
                        if (roomParticipants.length === 0) {
                          alert('Add team members first!');
                          return;
                        }
                        if (confirm('Finalize Emergency Transfer decision?\n\nThis will:\n• Lock the decision\n• Trigger workflows\n• Notify stakeholders\n• Create audit entry')) {
                          setActivityLog([{ user: 'System', time: 'Just now', action: '🎯 DECISION FINALIZED' }, ...activityLog]);
                          alert('✓ Decision finalized! Logistics execution triggered.');
                        }
                      }}
                      className="flex-1 min-w-[150px] px-4 py-2 bg-green-600 hover:bg-green-700 rounded font-semibold text-sm transition-all"
                    >
                      🎯 Finalize
                    </button>
                  </div>
                  {votingStatus && (
                    <div className="mt-4 p-3 bg-slate-900 rounded">
                      {votingStatus === 'inProgress' && (
                        <>
                          <strong>Voting in Progress...</strong>
                          <div className="text-sm text-slate-400 mt-2">Waiting for team members to cast their votes.</div>
                        </>
                      )}
                      {votingStatus === 'complete' && (
                        <>
                          <strong className="text-green-500">✓ Complete</strong>
                          <div className="text-sm text-slate-400 mt-2">Unanimous for Emergency Transfer</div>
                        </>
                      )}
                    </div>
                  )}
                </div>
              </div>
            </div>
          </div>
        )}

        {/* What-If Scenarios Tab */}
        {activeTab === 'what-if' && (
          <div className="bg-slate-800 rounded-lg p-6 border border-slate-700 max-w-4xl mx-auto">
            <h3 className="text-lg font-semibold mb-1">
              What-If Scenario Builder
              <span 
                onClick={() => setShowConfidenceModal(true)}
                className="ml-3 text-xs bg-slate-700 px-3 py-1.5 rounded cursor-help hover:bg-blue-600 transition-all inline-block"
              >
                Confidence: 87% | 47 scenarios
              </span>
            </h3>
            
            <div className="space-y-6 mt-6">
              <div>
                <label className="text-sm font-semibold mb-2 block">
                  Demand Change: <span className="text-blue-400">{demand > 0 ? '+' : ''}{demand}%</span>
                </label>
                <input
                  type="range"
                  min="-30"
                  max="50"
                  step="5"
                  value={demand}
                  onChange={(e) => setDemand(parseInt(e.target.value))}
                  className="w-full h-2 bg-slate-700 rounded-lg appearance-none cursor-pointer"
                />
                <div className="flex justify-between text-xs text-slate-500 mt-1">
                  <span>-30%</span>
                  <span>Baseline</span>
                  <span>+50%</span>
                </div>
              </div>

              <div>
                <label className="text-sm font-semibold mb-2 block">
                  Lead Time Change: <span className="text-blue-400">{leadtime > 0 ? '+' : ''}{leadtime} days</span>
                </label>
                <input
                  type="range"
                  min="-3"
                  max="5"
                  step="1"
                  value={leadtime}
                  onChange={(e) => setLeadtime(parseInt(e.target.value))}
                  className="w-full h-2 bg-slate-700 rounded-lg appearance-none cursor-pointer"
                />
                <div className="flex justify-between text-xs text-slate-500 mt-1">
                  <span>-3 days</span>
                  <span>Baseline</span>
                  <span>+5 days</span>
                </div>
              </div>

              <div>
                <label className="text-sm font-semibold mb-2 block">
                  Capacity Change: <span className="text-blue-400">{capacity > 0 ? '+' : ''}{capacity}%</span>
                </label>
                <input
                  type="range"
                  min="-20"
                  max="30"
                  step="5"
                  value={capacity}
                  onChange={(e) => setCapacity(parseInt(e.target.value))}
                  className="w-full h-2 bg-slate-700 rounded-lg appearance-none cursor-pointer"
                />
                <div className="flex justify-between text-xs text-slate-500 mt-1">
                  <span>-20%</span>
                  <span>Baseline</span>
                  <span>+30%</span>
                </div>
              </div>
            </div>

            <div className="grid grid-cols-3 gap-4 mt-8">
              <div className="col-span-2 bg-gradient-to-r from-blue-600 to-blue-700 rounded-lg p-8 text-center">
                <div className="text-sm mb-2 opacity-90">Projected OTIF Performance</div>
                <div className="text-6xl font-black mb-2">{projectedOTIF.toFixed(1)}%</div>
                <div className="text-sm opacity-85">
                  {Math.abs(projectedOTIF - 94.2) < 0.1 
                    ? 'Baseline' 
                    : projectedOTIF > 94.2 
                      ? `↗ +${(projectedOTIF - 94.2).toFixed(1)}% vs baseline`
                      : `↘ ${(projectedOTIF - 94.2).toFixed(1)}% vs baseline`
                  }
                </div>
              </div>
              <div className="flex flex-col gap-3">
                <button 
                  onClick={() => {
                    setDemand(0);
                    setLeadtime(0);
                    setCapacity(0);
                  }}
                  className="px-4 py-3 bg-slate-600 hover:bg-slate-500 rounded font-semibold transition-all"
                >
                  ↺ Reset
                </button>
              </div>
            </div>

            <div className="mt-6 p-5 bg-slate-900 rounded">
              <div className="text-sm font-semibold text-blue-400 mb-3">🤖 AI Recommendation</div>
              <div className="text-sm leading-relaxed">
                {projectedOTIF < 90 
                  ? 'Consider increasing capacity or reducing lead times to maintain service levels.'
                  : projectedOTIF > 97
                    ? 'Scenario shows strong performance. Consider cost optimization opportunities.'
                    : 'Projected performance within acceptable range. Monitor closely for early warning signals.'}
              </div>
            </div>
          </div>
        )}

        {/* Retailer Collaboration Tab */}
        {activeTab === 'retailer-collab' && (
          <div>
            <div className="bg-slate-800 rounded-lg p-5 border border-slate-700 mb-5">
              <h3 className="text-lg font-semibold mb-2">🤝 Joint Decision Room: Manufacturer ↔ Retailer</h3>
              <p className="text-sm text-slate-400">Collaborative planning with shared visibility</p>
            </div>

            <div className="grid grid-cols-2 gap-5 mb-5">
              {/* Manufacturer Panel */}
              <div className="bg-slate-800 rounded-lg p-5 border border-slate-700">
                <div className="flex items-center gap-3 mb-5 pb-4 border-b-2 border-slate-700">
                  <div className="w-12 h-12 bg-gradient-to-br from-blue-600 to-blue-700 rounded-lg flex items-center justify-center text-2xl">
                    🏭
                  </div>
                  <div>
                    <h3 className="font-bold text-lg">Manufacturer View</h3>
                    <p className="text-xs text-slate-400">Nordic Beverages Co.</p>
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-3 mb-5">
                  {[
                    { label: 'Production Capacity', value: '87%' },
                    { label: 'Available Inventory', value: '18.4K' },
                    { label: 'Lead Time', value: '36h' },
                    { label: 'OTIF Current', value: '96.5%', color: 'text-green-500' }
                  ].map((metric, idx) => (
                    <div key={idx} className="bg-slate-900 rounded p-3">
                      <div className="text-xs text-slate-400 mb-1">{metric.label}</div>
                      <div className={`text-xl font-extrabold ${metric.color || ''}`}>{metric.value}</div>
                    </div>
                  ))}
                </div>

                <h4 className="text-sm font-semibold text-slate-400 mb-3">Supply Constraints</h4>
                <div className="space-y-2 text-sm">
                  <div className="bg-slate-900 p-3 rounded">
                    <strong className="text-yellow-500">⚠️ Denmark Warehouse:</strong> 13.2K cases available, 36h transfer time
                  </div>
                  <div className="bg-slate-900 p-3 rounded">
                    <strong className="text-blue-400">ℹ️ Production:</strong> Next batch in 72h, capacity at 87%
                  </div>
                  <div className="bg-slate-900 p-3 rounded">
                    <strong className="text-green-500">✓ Flexibility:</strong> Can expedite for priority customers
                  </div>
                </div>
              </div>

              {/* Retailer Panel */}
              <div className="bg-slate-800 rounded-lg p-5 border border-slate-700">
                <div className="flex items-center gap-3 mb-5 pb-4 border-b-2 border-slate-700">
                  <div className="w-12 h-12 bg-gradient-to-br from-green-600 to-green-700 rounded-lg flex items-center justify-center text-2xl">
                    🏪
                  </div>
                  <div>
                    <h3 className="font-bold text-lg">Retailer View</h3>
                    <p className="text-xs text-slate-400">SuperMart Norway</p>
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-3 mb-5">
                  {[
                    { label: 'Current Stock', value: '2.1K', color: 'text-red-500' },
                    { label: 'Daily Demand', value: '1.8K' },
                    { label: 'Days of Supply', value: '1.2', color: 'text-red-500' },
                    { label: 'Promo Impact', value: '+35%', color: 'text-yellow-500' }
                  ].map((metric, idx) => (
                    <div key={idx} className="bg-slate-900 rounded p-3">
                      <div className="text-xs text-slate-400 mb-1">{metric.label}</div>
                      <div className={`text-xl font-extrabold ${metric.color || ''}`}>{metric.value}</div>
                    </div>
                  ))}
                </div>

                <h4 className="text-sm font-semibold text-slate-400 mb-3">Demand Insights</h4>
                <div className="space-y-2 text-sm">
                  <div className="bg-slate-900 p-3 rounded">
                    <strong className="text-red-500">🔥 Urgent:</strong> Stockout risk in 1.2 days
                  </div>
                  <div className="bg-slate-900 p-3 rounded">
                    <strong className="text-yellow-500">📈 Trend:</strong> Demand +35% (sports event)
                  </div>
                  <div className="bg-slate-900 p-3 rounded">
                    <strong className="text-green-500">💰 Opportunity:</strong> €42K revenue if supply maintained
                  </div>
                </div>
              </div>
            </div>

            {/* Shared Insights */}
            <div className="bg-slate-900 rounded-lg p-6 border border-slate-700">
              <h4 className="text-lg font-semibold text-blue-400 mb-4">🎯 Shared Decision Insights</h4>
              
              <div className="space-y-3 mb-5">
                {[
                  { icon: '✓', iconBg: 'from-green-600 to-green-700', title: 'Aligned: Emergency Transfer', desc: 'Win-win: Manufacturer saves €26.4K, Retailer maintains availability. ROI: 5.2x', titleColor: 'text-green-500' },
                  { icon: '📊', iconBg: 'from-blue-600 to-blue-700', title: 'Joint Forecast: 94.2%', desc: 'Combined data improves accuracy by 12% vs. standalone' },
                  { icon: '⚡', iconBg: 'from-yellow-600 to-yellow-700', title: 'Timeline Agreement', desc: 'Manufacturer: 36h delivery | Retailer: Promotional extension' }
                ].map((insight, idx) => (
                  <div key={idx} className="flex items-start gap-3 p-3 bg-slate-800 rounded">
                    <div className={`w-8 h-8 bg-gradient-to-br ${insight.iconBg} rounded-full flex items-center justify-center flex-shrink-0`}>
                      {insight.icon}
                    </div>
                    <div>
                      <strong className={insight.titleColor || ''}>{insight.title}</strong>
                      <div className="text-sm text-slate-400 mt-1">{insight.desc}</div>
                    </div>
                  </div>
                ))}
              </div>

              <div className="flex gap-3 flex-wrap">
                <button 
                  onClick={() => {
                    if (confirm('Approve joint decision?\n\n• Emergency transfer: 13.2K cases\n• 36-hour delivery\n• Promotional extension\n• €26.4K value')) {
                      alert('✓ Joint decision approved!\n\nNext:\n1. Contract generation\n2. Logistics notified\n3. Retailer alerted\n4. KPI dashboard activated');
                    }
                  }}
                  className="flex-1 min-w-[150px] px-4 py-3 bg-green-600 hover:bg-green-700 rounded font-semibold transition-all"
                >
                  ✓ Both Approve
                </button>
                <button 
                  onClick={() => alert('Negotiation room opened.\n\nTopics:\n• Delivery timing\n• Cost sharing\n• Promotional timeline\n• Collaboration framework')}
                  className="flex-1 min-w-[150px] px-4 py-3 bg-yellow-600 hover:bg-yellow-700 rounded font-semibold transition-all"
                >
                  💬 Negotiate
                </button>
                <button className="flex-1 min-w-[150px] px-4 py-3 bg-slate-600 hover:bg-slate-500 rounded font-semibold transition-all">
                  📄 Contract
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Similar Cases Tab */}
        {activeTab === 'similar-cases' && (
          <div className="space-y-6">
            <div className="bg-gradient-to-r from-blue-600 to-blue-700 rounded-lg p-5 border border-blue-500">
              <h3 className="text-lg font-semibold mb-2">AI-Powered Case Matching</h3>
              <div className="text-sm opacity-90">
                Found 47 similar situations from historical data. Top 3 matches shown below.
              </div>
            </div>

            <div className="space-y-4">
              {[
                { id: 'NORD-2024-06-15', desc: 'Sweden summer demand surge', decision: 'Emergency transfer + Air freight', otif: '96.8%', cost: '€7,200', value: '€31,500', similarity: '94%' },
                { id: 'NORD-2023-12-03', desc: 'Denmark holiday spike', decision: 'Regional inventory rebalancing', otif: '98.1%', cost: '€3,840', value: '€28,900', similarity: '92%' },
                { id: 'NORD-2023-11-08', desc: 'Norway stockout risk', decision: 'Emergency transfer', otif: '97.2%', cost: '€5,120', value: '€22,400', similarity: '88%' }
              ].map((caseItem, idx) => (
                <div key={idx} className="bg-slate-800 rounded-lg p-5 border border-slate-700 grid grid-cols-12 gap-5 items-center">
                  <div className="col-span-3">
                    <div className="text-xs text-slate-400 mb-1">CASE ID</div>
                    <div className="text-base font-bold font-mono">{caseItem.id}</div>
                    <div className="text-xs text-slate-500 mt-2">{caseItem.desc}</div>
                  </div>
                  <div className="col-span-6">
                    <div className="text-xs text-slate-400 mb-2">DECISION & OUTCOME</div>
                    <div className="font-semibold mb-2">{caseItem.decision}</div>
                    <div className="flex gap-4 text-xs">
                      <span>OTIF: <strong>{caseItem.otif}</strong></span>
                      <span>Cost: <strong>{caseItem.cost}</strong></span>
                      <span>Value: <strong className="text-green-500">{caseItem.value}</strong></span>
                    </div>
                  </div>
                  <div className="col-span-3 text-right">
                    <div className="text-xs text-slate-400 mb-1">SIMILARITY</div>
                    <div className="text-4xl font-extrabold text-blue-500">{caseItem.similarity}</div>
                  </div>
                </div>
              ))}
            </div>

            <div className="bg-gradient-to-r from-blue-600 to-blue-700 rounded-lg p-5 text-center">
              <div className="font-semibold mb-2">
                <strong>Learning Pattern:</strong> Nordic region transfer operations show 6.2x average ROI across 47 cases
              </div>
              <div className="text-sm opacity-90">
                Success rate: 94% | Average recovery time: 52 hours | Median cost: €4,600
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Confidence Modal */}
      {showConfidenceModal && (
        <div 
          className="fixed inset-0 bg-black/80 flex items-center justify-center p-5 z-50"
          onClick={() => setShowConfidenceModal(false)}
        >
          <div 
            className="bg-slate-800 rounded-xl p-8 max-w-2xl w-full max-h-[90vh] overflow-y-auto border-2 border-slate-700 relative"
            onClick={(e) => e.stopPropagation()}
          >
            <button
              onClick={() => setShowConfidenceModal(false)}
              className="absolute top-4 right-4 w-8 h-8 bg-slate-700 hover:bg-slate-600 rounded-full flex items-center justify-center text-xl transition-colors"
            >
              ×
            </button>

            <div className="mb-6">
              <h2 className="text-2xl font-bold mb-2">Understanding Confidence Score</h2>
              <p className="text-sm text-slate-400">How we calculate 87% confidence</p>
            </div>

            <div className="bg-gradient-to-r from-blue-600 to-blue-700 rounded-lg p-8 text-center mb-6">
              <div className="text-7xl font-black mb-2">87%</div>
              <div className="text-sm opacity-90">Prediction Confidence</div>
            </div>

            <div className="mb-6">
              <h4 className="text-lg font-semibold text-blue-400 mb-3">📊 Data Sources</h4>
              <div className="space-y-3">
                {[
                  { icon: '47', title: 'Similar Historical Scenarios', desc: 'Matched cases from past 18 months with 85%+ similarity' },
                  { icon: '94%', title: 'Model Accuracy', desc: 'Cross-validated on Nordic region supply chain events' },
                  { icon: '12', title: 'Key Variables Analyzed', desc: 'Demand volatility, inventory, lead times, regional factors' }
                ].map((item, idx) => (
                  <div key={idx} className="flex items-center gap-3 p-3 bg-slate-900 rounded">
                    <div className="w-10 h-10 bg-slate-700 rounded-full flex items-center justify-center font-bold text-blue-400 flex-shrink-0">
                      {item.icon}
                    </div>
                    <div>
                      <strong className="block">{item.title}</strong>
                      <small className="text-slate-400">{item.desc}</small>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <div className="mb-6">
              <h4 className="text-lg font-semibold text-blue-400 mb-3">🎯 Confidence Breakdown</h4>
              <div className="space-y-3">
                {[
                  { label: 'Scenario Similarity', value: 92, color: 'from-green-600 to-green-700' },
                  { label: 'Success Rate', value: 94, color: 'from-green-600 to-green-700' },
                  { label: 'Data Quality', value: 89, color: 'from-green-600 to-green-700' },
                  { label: 'External Stability', value: 78, color: 'from-yellow-600 to-yellow-700' }
                ].map((item, idx) => (
                  <div key={idx}>
                    <div className="flex justify-between text-sm mb-1">
                      <span>{item.label}</span>
                      <strong>{item.value}%</strong>
                    </div>
                    <div className="w-full h-2 bg-slate-700 rounded-full overflow-hidden">
                      <div 
                        className={`h-full bg-gradient-to-r ${item.color}`}
                        style={{ width: `${item.value}%` }}
                      ></div>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <div className="mb-6">
              <h4 className="text-lg font-semibold text-blue-400 mb-3">⚠️ Uncertainty Factors</h4>
              <div className="bg-red-900/20 border-l-4 border-red-500 p-4 rounded">
                <ul className="list-disc list-inside text-sm space-y-1">
                  <li>Weather in transit route (±8% impact)</li>
                  <li>Warehouse staff availability (±5%)</li>
                  <li>Competing priority orders (±4%)</li>
                </ul>
              </div>
            </div>

            <div className="bg-blue-900/20 border-l-4 border-blue-500 p-4 rounded">
              <strong className="text-blue-400">Interpretation:</strong> 87% confidence indicates high reliability based on substantial historical evidence and proven patterns.
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

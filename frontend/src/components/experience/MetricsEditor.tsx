import React, { useState } from 'react';

interface MetricsEditorProps {
  value: Record<string, unknown> | null | undefined;
  onChange: (metrics: Record<string, unknown>) => void;
}

export const MetricsEditor: React.FC<MetricsEditorProps> = ({ value, onChange }) => {
  const metrics = value || {};
  const entries = Object.entries(metrics);
  const [newKey, setNewKey] = useState('');
  const [newValue, setNewValue] = useState('');

  const handleAdd = () => {
    if (!newKey.trim()) return;
    const updated = { ...metrics, [newKey.trim()]: newValue.trim() };
    onChange(updated);
    setNewKey('');
    setNewValue('');
  };

  const handleRemove = (key: string) => {
    const updated = { ...metrics };
    delete updated[key];
    onChange(updated);
  };

  return (
    <div className="space-y-2">
      <label className="block text-sm font-medium text-gray-700">Metrics</label>

      {entries.map(([key, val]) => (
        <div key={key} className="flex items-center space-x-2">
          <span className="text-sm font-medium text-gray-600 min-w-[100px]">{key}:</span>
          <span className="text-sm text-gray-800 flex-1">{String(val)}</span>
          <button
            type="button"
            onClick={() => handleRemove(key)}
            className="text-red-500 hover:text-red-700 text-sm"
          >
            Remove
          </button>
        </div>
      ))}

      <div className="flex items-center space-x-2">
        <input
          type="text"
          placeholder="Key (e.g. TPS)"
          value={newKey}
          onChange={(e) => setNewKey(e.target.value)}
          className="px-2 py-1 border border-gray-300 rounded text-sm flex-1"
        />
        <input
          type="text"
          placeholder="Value (e.g. 15000)"
          value={newValue}
          onChange={(e) => setNewValue(e.target.value)}
          className="px-2 py-1 border border-gray-300 rounded text-sm flex-1"
        />
        <button
          type="button"
          onClick={handleAdd}
          className="px-3 py-1 bg-gray-200 text-gray-700 rounded text-sm hover:bg-gray-300"
        >
          Add
        </button>
      </div>
    </div>
  );
};

import { useMemo, useCallback, useEffect, useState } from 'react';
import {
  ReactFlow,
  Background,
  Controls,
  useNodesState,
  useEdgesState,
  type Node,
  type Edge,
  type NodeTypes,
  Handle,
  Position,
  useReactFlow,
  ReactFlowProvider,
} from '@xyflow/react';
import ELK from 'elkjs/lib/elk.bundled.js';
import '@xyflow/react/dist/style.css';
import type { ERDTableInfo, ERDRelationship, ColumnInfo } from '../../types/database';
import './ERDViewer.css';

// Node dimensions for layout calculation
const NODE_WIDTH = 250;
const NODE_HEIGHT_BASE = 40; // Header height
const NODE_HEIGHT_PER_COLUMN = 28; // Height per column row

// Calculate node height based on number of columns
function getNodeHeight(columnCount: number): number {
  return NODE_HEIGHT_BASE + columnCount * NODE_HEIGHT_PER_COLUMN;
}

// ELK instance
const elk = new ELK();

// ELK layout options for minimizing edge crossings
const elkOptions = {
  'elk.algorithm': 'layered',
  'elk.direction': 'RIGHT',
  'elk.spacing.nodeNode': '80',
  'elk.layered.spacing.nodeNodeBetweenLayers': '100',
  'elk.layered.crossingMinimization.strategy': 'LAYER_SWEEP',
  'elk.layered.crossingMinimization.greedySwitch.type': 'TWO_SIDED',
  'elk.layered.nodePlacement.strategy': 'NETWORK_SIMPLEX',
  'elk.layered.considerModelOrder.strategy': 'NODES_AND_EDGES',
  'elk.edgeRouting': 'ORTHOGONAL',
  'elk.layered.unnecessaryBendpoints': 'true',
};

// Apply ELK layout algorithm to nodes (async)
async function getLayoutedElements(
  nodes: Node[],
  edges: Edge[],
  tables: ERDTableInfo[]
): Promise<Node[]> {
  const graph = {
    id: 'root',
    layoutOptions: elkOptions,
    children: nodes.map((node) => {
      const table = tables.find((t) => t.name === node.id);
      const height = table ? getNodeHeight(table.columns.length) : NODE_HEIGHT_BASE;
      return {
        id: node.id,
        width: NODE_WIDTH,
        height,
      };
    }),
    edges: edges
      .filter((edge) => edge.source !== edge.target) // Skip self-referencing
      .map((edge) => ({
        id: edge.id,
        sources: [edge.source],
        targets: [edge.target],
      })),
  };

  const layoutedGraph = await elk.layout(graph);

  return nodes.map((node) => {
    const layoutedNode = layoutedGraph.children?.find((n) => n.id === node.id);
    return {
      ...node,
      position: {
        x: layoutedNode?.x ?? 0,
        y: layoutedNode?.y ?? 0,
      },
    };
  });
}

// SVG Marker definitions for ERD relationship notation (crow's foot)
function ERDMarkerDefinitions() {
  return (
    <svg style={{ position: 'absolute', width: 0, height: 0 }}>
      <defs>
        {/* "One" marker - single perpendicular line */}
        <marker
          id="erd-one"
          viewBox="0 0 20 20"
          markerWidth="10"
          markerHeight="10"
          refX="15"
          refY="10"
          orient="auto-start-reverse"
        >
          <line x1="15" y1="4" x2="15" y2="16" stroke="#6b7280" strokeWidth="2" />
        </marker>

        {/* "Many" marker - crow's foot */}
        <marker
          id="erd-many"
          viewBox="0 0 20 20"
          markerWidth="10"
          markerHeight="10"
          refX="15"
          refY="10"
          orient="auto-start-reverse"
        >
          <line x1="5" y1="10" x2="15" y2="4" stroke="#6b7280" strokeWidth="2" />
          <line x1="5" y1="10" x2="15" y2="10" stroke="#6b7280" strokeWidth="2" />
          <line x1="5" y1="10" x2="15" y2="16" stroke="#6b7280" strokeWidth="2" />
        </marker>

        {/* "One" marker for start (reversed direction) */}
        <marker
          id="erd-one-start"
          viewBox="0 0 20 20"
          markerWidth="10"
          markerHeight="10"
          refX="5"
          refY="10"
          orient="auto"
        >
          <line x1="5" y1="4" x2="5" y2="16" stroke="#6b7280" strokeWidth="2" />
        </marker>

        {/* "Many" marker for start (reversed direction) */}
        <marker
          id="erd-many-start"
          viewBox="0 0 20 20"
          markerWidth="10"
          markerHeight="10"
          refX="5"
          refY="10"
          orient="auto"
        >
          <line x1="15" y1="10" x2="5" y2="4" stroke="#6b7280" strokeWidth="2" />
          <line x1="15" y1="10" x2="5" y2="10" stroke="#6b7280" strokeWidth="2" />
          <line x1="15" y1="10" x2="5" y2="16" stroke="#6b7280" strokeWidth="2" />
        </marker>
      </defs>
    </svg>
  );
}

// Get markers based on relationship type
function getRelationshipMarkers(relationshipType: 'one-to-one' | 'one-to-many' | 'many-to-one') {
  switch (relationshipType) {
    case 'one-to-one':
      return { markerStart: 'url(#erd-one-start)', markerEnd: 'url(#erd-one)' };
    case 'one-to-many':
      return { markerStart: 'url(#erd-one-start)', markerEnd: 'url(#erd-many)' };
    case 'many-to-one':
      return { markerStart: 'url(#erd-many-start)', markerEnd: 'url(#erd-one)' };
    default:
      return { markerStart: 'url(#erd-one-start)', markerEnd: 'url(#erd-many)' };
  }
}

interface ERDViewerProps {
  tables: ERDTableInfo[];
  relationships: ERDRelationship[];
}

// Color palette for table headers
const TABLE_COLORS = [
  '#3b82f6', // blue
  '#22c55e', // green
  '#eab308', // yellow
  '#a855f7', // purple
  '#ec4899', // pink
  '#f97316', // orange
  '#06b6d4', // cyan
  '#ef4444', // red
];

// Get type color for column types
function getTypeColor(type: string): string {
  const upper = type.toUpperCase();
  if (upper.includes('INT')) return '#3b82f6';
  if (upper.includes('TEXT') || upper.includes('CHAR')) return '#22c55e';
  if (upper.includes('REAL') || upper.includes('FLOAT')) return '#f59e0b';
  if (upper.includes('BLOB')) return '#8b5cf6';
  if (upper.includes('BOOL')) return '#ec4899';
  return '#6b7280';
}

// Custom Table Node Component
interface TableNodeData {
  label: string;
  columns: ColumnInfo[];
  color: string;
}

function TableNode({ data }: { data: TableNodeData }) {
  return (
    <div className="erd-table-node">
      <div className="erd-table-header" style={{ backgroundColor: data.color }}>
        <span className="erd-table-name">{data.label}</span>
      </div>
      <div className="erd-table-columns">
        {data.columns.map((col, index) => (
          <div key={col.name} className="erd-column-row">
            <Handle
              type="target"
              position={Position.Left}
              id={`${col.name}-target`}
              style={{ top: 15 + index * 0, background: '#6b7280' }}
            />
            {/* Left-side source handle for self-referencing relationships */}
            <Handle
              type="source"
              position={Position.Left}
              id={`${col.name}-source-left`}
              style={{ top: 15 + index * 0, background: '#6b7280' }}
            />
            <div className="erd-column-content">
              {col.pk > 0 && (
                <svg className="erd-pk-icon" viewBox="0 0 24 24" fill="#f59e0b" width="12" height="12">
                  <path d="M12 2C9.24 2 7 4.24 7 7c0 1.8.96 3.37 2.4 4.24L7 22h10l-2.4-10.76C16.04 10.37 17 8.8 17 7c0-2.76-2.24-5-5-5zm0 7c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2z" />
                </svg>
              )}
              <span className="erd-column-name">{col.name}</span>
              <span className="erd-column-type" style={{ color: getTypeColor(col.type) }}>
                {col.type}
              </span>
            </div>
            <Handle
              type="source"
              position={Position.Right}
              id={`${col.name}-source`}
              style={{ top: 15 + index * 0, background: '#6b7280' }}
            />
          </div>
        ))}
      </div>
    </div>
  );
}

const nodeTypes: NodeTypes = {
  tableNode: TableNode,
};

function ERDViewerInner({ tables, relationships }: ERDViewerProps) {
  const { fitView } = useReactFlow();
  const [layoutReady, setLayoutReady] = useState(false);

  // Transform relationships to ReactFlow edges
  const initialEdges: Edge[] = useMemo(() => {
    return relationships.map((rel, index) => {
      const isSelfReferencing = rel.fromTable === rel.toTable;
      const markers = getRelationshipMarkers(rel.relationshipType);

      return {
        id: `edge-${index}`,
        source: rel.fromTable,
        target: rel.toTable,
        // For self-referencing: use left-side source, left-side target
        sourceHandle: isSelfReferencing
          ? `${rel.fromColumn}-source-left`
          : `${rel.fromColumn}-source`,
        targetHandle: `${rel.toColumn}-target`,
        type: 'smoothstep',
        animated: false,
        style: {
          stroke: '#6b7280',
          strokeWidth: 2,
          markerStart: markers.markerStart,
          markerEnd: markers.markerEnd,
        },
      };
    });
  }, [relationships]);

  // Create initial nodes without layout
  const initialNodes: Node[] = useMemo(() => {
    return tables.map((table, index) => ({
      id: table.name,
      type: 'tableNode',
      position: { x: 0, y: 0 }, // Will be set by ELK layout
      data: {
        label: table.name,
        columns: table.columns,
        color: TABLE_COLORS[index % TABLE_COLORS.length],
      },
    }));
  }, [tables]);

  const [nodes, setNodes, onNodesChange] = useNodesState(initialNodes);
  const [edges, setEdges, onEdgesChange] = useEdgesState(initialEdges);

  // Apply ELK layout asynchronously
  useEffect(() => {
    const applyLayout = async () => {
      if (initialNodes.length === 0) return;

      setLayoutReady(false);
      const layoutedNodes = await getLayoutedElements(initialNodes, initialEdges, tables);
      setNodes(layoutedNodes);
      setEdges(initialEdges);
      setLayoutReady(true);

      // Fit view after layout is applied
      setTimeout(() => {
        fitView({ padding: 0.2 });
      }, 50);
    };

    applyLayout();
  }, [initialNodes, initialEdges, tables, setNodes, setEdges, fitView]);

  const onInit = useCallback(() => {
    console.log('ReactFlow initialized');
  }, []);

  return (
    <>
      <ERDMarkerDefinitions />
      <div className="erd-toolbar">
        <span className="erd-info">
          {tables.length} table{tables.length !== 1 ? 's' : ''}, {relationships.length} relationship{relationships.length !== 1 ? 's' : ''}
        </span>
        <span className="erd-hint">Drag tables to reposition. Scroll to zoom. Drag background to pan.</span>
      </div>
      <div className="erd-flow-container" style={{ opacity: layoutReady ? 1 : 0 }}>
        <ReactFlow
          nodes={nodes}
          edges={edges}
          onNodesChange={onNodesChange}
          onEdgesChange={onEdgesChange}
          onInit={onInit}
          nodeTypes={nodeTypes}
          fitView
          fitViewOptions={{ padding: 0.2 }}
          minZoom={0.1}
          maxZoom={2}
          defaultEdgeOptions={{
            type: 'smoothstep',
          }}
        >
          <Background color="#374151" gap={20} />
          <Controls />
        </ReactFlow>
      </div>
    </>
  );
}

export default function ERDViewer({ tables, relationships }: ERDViewerProps) {
  if (tables.length === 0) {
    return (
      <div className="erd-empty">
        <p>No tables to display</p>
      </div>
    );
  }

  return (
    <div className="erd-viewer">
      <ReactFlowProvider>
        <ERDViewerInner tables={tables} relationships={relationships} />
      </ReactFlowProvider>
    </div>
  );
}

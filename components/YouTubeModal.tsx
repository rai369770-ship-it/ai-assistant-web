import React, { useState } from 'react';
import { X, Youtube, Plus } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';

interface YouTubeModalProps {
  isOpen: boolean;
  onClose: () => void;
  onAdd: (url: string) => void;
}

export const YouTubeModal: React.FC<YouTubeModalProps> = ({ isOpen, onClose, onAdd }) => {
  const [url, setUrl] = useState('');
  const [error, setError] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!url.startsWith('https://')) {
      setError('URL must start with https://');
      return;
    }
    onAdd(url);
    setUrl('');
    setError('');
    onClose();
  };

  return (
    <AnimatePresence>
      {isOpen && (
        <motion.div 
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          className="fixed inset-0 z-[60] flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm"
        >
          <motion.div 
            initial={{ scale: 0.95, opacity: 0, y: 20 }}
            animate={{ scale: 1, opacity: 1, y: 0 }}
            exit={{ scale: 0.95, opacity: 0, y: 20 }}
            className="bg-surface w-full max-w-md rounded-2xl border border-gray-700 shadow-2xl overflow-hidden"
          >
            <div className="flex items-center justify-between p-4 border-b border-gray-700 bg-gray-900/50">
              <h3 className="flex items-center gap-2 font-bold text-gray-100">
                <Youtube size={20} className="text-red-500" />
                Add YouTube Link
              </h3>
              <motion.button 
                whileHover={{ scale: 1.1 }}
                whileTap={{ scale: 0.9 }}
                onClick={onClose} 
                className="p-1 hover:bg-gray-800 rounded-full text-gray-400 hover:text-white transition-colors"
              >
                <X size={20} />
              </motion.button>
            </div>
            
            <form onSubmit={handleSubmit} className="p-6">
              <label className="block text-sm font-medium text-gray-300 mb-2">
                Enter a YouTube video to summarize or transcribe with AI assistant.
              </label>
              <input
                autoFocus
                type="url"
                value={url}
                onChange={(e) => { setUrl(e.target.value); setError(''); }}
                placeholder="https://www.youtube.com/watch?v=..."
                className={`w-full bg-background border rounded-xl px-4 py-3 text-gray-100 placeholder-gray-500 outline-none transition-all ${error ? 'border-red-500 focus:ring-1 focus:ring-red-500' : 'border-gray-700 focus:border-primary focus:ring-1 focus:ring-primary/20'}`}
              />
              {error && <p className="mt-2 text-xs text-red-500 font-medium">{error}</p>}
              
              <div className="mt-6 flex justify-end gap-3">
                <motion.button
                  whileHover={{ scale: 1.05 }}
                  whileTap={{ scale: 0.95 }}
                  type="button"
                  onClick={onClose}
                  className="flex items-center gap-2 px-4 py-2 text-sm font-medium text-gray-400 hover:text-white transition-colors"
                >
                  <X size={16} />
                  Cancel
                </motion.button>
                <motion.button
                  whileHover={url.trim() ? { scale: 1.05 } : {}}
                  whileTap={url.trim() ? { scale: 0.95 } : {}}
                  type="submit"
                  disabled={!url.trim()}
                  className="flex items-center gap-2 px-6 py-2 bg-primary hover:bg-blue-600 disabled:bg-gray-800 disabled:text-gray-500 rounded-xl text-sm font-bold text-white transition-colors shadow-lg shadow-blue-500/20"
                >
                  <Plus size={16} />
                  Add Link
                </motion.button>
              </div>
            </form>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
};
import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../../api.js';

export default function ReviewRequests() {
  const navigate = useNavigate();
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  
  // States cho modal kiểm chứng chi tiết (Audit / Review)
  const [selectedRequest, setSelectedRequest] = useState(null);
  const [auditDetails, setAuditDetails] = useState(null);
  const [loadingAudit, setLoadingAudit] = useState(false);
  const [feedbackComment, setFeedbackComment] = useState('');
  const [submittingFeedback, setSubmittingFeedback] = useState(false);
  const [toastMessage, setToastMessage] = useState('');

  // 1. Tải danh sách các feedback requests và lấy tên dự án thực tế
  const fetchRequests = async () => {
    try {
      setLoading(true);
      setError('');
      const res = await api.get('/api/feedback-requests');
      // Lọc các yêu cầu đang chờ duyệt (status: PENDING)
      const pendingRequests = (res.data || []).filter(req => req.status === 'PENDING');
      
      // Với mỗi yêu cầu, gọi API export để lấy tên dự án thực tế
      const requestsWithDetails = await Promise.all(
        pendingRequests.map(async (req) => {
          try {
            const exportRes = await api.get(`/api/projects/${req.projectId}/traceability-export`);
            return {
              ...req,
              projectTitle: exportRes.data.projectTitle || `Dự án ID: ${req.projectId}`,
              projectStatus: exportRes.data.projectStatus
            };
          } catch (err) {
            console.error(`Failed to fetch project title for ${req.projectId}`, err);
            return {
              ...req,
              projectTitle: `Dự án ID: ${req.projectId}`
            };
          }
        })
      );
      
      setRequests(requestsWithDetails);
    } catch (err) {
      console.error('Failed to fetch feedback requests:', err);
      setError('Không thể kết nối đến máy chủ Backend để lấy danh sách yêu cầu.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRequests();
  }, []);

  // 2. Mở Modal đánh giá dự án
  const handleOpenAudit = async (req) => {
    setSelectedRequest(req);
    setFeedbackComment('');
    setAuditDetails(null);
    setLoadingAudit(true);
    try {
      const exportRes = await api.get(`/api/projects/${req.projectId}/traceability-export`);
      setAuditDetails(exportRes.data);
    } catch (err) {
      console.error('Failed to fetch traceability details for audit:', err);
      showToast('Không thể tải thông tin kiểm chứng của dự án này.');
    } finally {
      setLoadingAudit(false);
    }
  };

  // 3. Thực hiện lưu feedback và chuyển đổi trạng thái duyệt dự án
  const handleReviewAction = async (actionType) => {
    if (!selectedRequest) return;
    
    // Nếu chuyển trả về hoặc từ chối, yêu cầu giảng viên phải ghi nhận xét
    if ((actionType === 'RETURNED' || actionType === 'REJECTED') && !feedbackComment.trim()) {
      alert('Vui lòng nhập nhận xét/lý do trả lại hoặc từ chối dự án này.');
      return;
    }

    try {
      setSubmittingFeedback(true);
      
      // Bước A: Gửi feedback comment nếu có nội dung nhận xét
      if (feedbackComment.trim()) {
        await api.post(`/api/feedback-requests/${selectedRequest.id}/feedback`, {
          content: feedbackComment.trim()
        });
      }

      // Bước B: Gọi API chuyển đổi trạng thái của yêu cầu
      let endpoint = '';
      let actionLabel = '';
      if (actionType === 'APPROVED') {
        endpoint = `/api/feedback-requests/${selectedRequest.id}/reviewed`;
        actionLabel = 'phê duyệt thành công';
      } else if (actionType === 'REJECTED') {
        endpoint = `/api/feedback-requests/${selectedRequest.id}/rejected`;
        actionLabel = 'từ chối';
      } else if (actionType === 'RETURNED') {
        endpoint = `/api/feedback-requests/${selectedRequest.id}/return-to-active`;
        actionLabel = 'trả lại để chỉnh sửa';
      }

      await api.post(endpoint);
      showToast(`Yêu cầu của dự án đã được ${actionLabel}!`);
      
      // Đóng modal và tải lại danh sách
      setSelectedRequest(null);
      setAuditDetails(null);
      await fetchRequests();
    } catch (err) {
      console.error(`Failed to execute review action ${actionType}:`, err);
      alert('Thao tác phê duyệt thất bại. Vui lòng kiểm tra lại quyền truy cập.');
    } finally {
      setSubmittingFeedback(false);
    }
  };

  const showToast = (msg) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(''), 3000);
  };

  return (
    <div className="min-h-screen bg-slate-50 font-sans text-slate-800 antialiased">
      {/* Header */}
      <header className="bg-indigo-900 text-white border-b border-indigo-950 sticky top-0 z-30 shadow-md">
        <div className="w-full px-8 h-16 flex items-center justify-between">
          <div className="flex items-center space-x-3 cursor-pointer" onClick={() => navigate('/instructor/dashboard')}>
            <div className="w-7 h-7 bg-white text-indigo-900 rounded-md text-xs flex items-center justify-center font-black shadow-sm">EP</div>
            <span className="font-bold text-xl tracking-wider">Evidence Pilot</span>
          </div>
          <div className="flex items-center space-x-6">
            <div className="flex items-center space-x-2 bg-white/10 px-3 py-1.5 rounded border border-white/20">
              <div className="w-2 h-2 rounded-full bg-green-400 animate-pulse"></div>
              <span className="text-xs font-semibold text-indigo-100 tracking-wide uppercase">Giảng Viên</span>
            </div>
            <button 
              onClick={() => navigate('/instructor/dashboard')}
              className="text-sm font-semibold text-indigo-200 hover:text-white transition"
            >
              Bảng điều khiển
            </button>
            <button 
              onClick={() => {
                localStorage.removeItem('token');
                localStorage.removeItem('role');
                navigate('/login');
              }}
              className="text-sm font-semibold text-rose-300 hover:text-rose-100 transition"
            >
              Đăng xuất
            </button>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-6xl mx-auto px-6 py-10">
        <div className="flex justify-between items-center mb-8">
          <div>
            <h2 className="text-2xl font-bold text-slate-800">Duyệt Yêu Cầu Từ Học Sinh</h2>
            <p className="text-sm text-slate-500 mt-1">Xem xét các luận điểm và đối chiếu chứng cứ do học sinh gửi lên</p>
          </div>
          <span className="px-3.5 py-1 bg-amber-100 text-amber-800 rounded-full text-xs font-bold border border-amber-200 flex items-center gap-1.5 shadow-sm">
            <div className="w-1.5 h-1.5 rounded-full bg-amber-500 animate-pulse"></div>
            {requests.length} yêu cầu đang chờ
          </span>
        </div>

        {error && (
          <div className="bg-rose-50 border border-rose-200 text-rose-700 p-4 rounded-xl text-sm mb-6 flex items-center justify-between">
            <span>{error}</span>
            <button onClick={fetchRequests} className="px-3 py-1 bg-rose-600 text-white rounded-lg text-xs font-bold hover:bg-rose-700 transition">Thử lại</button>
          </div>
        )}

        {loading ? (
          <div className="text-center py-16 bg-white border border-slate-200 rounded-2xl shadow-sm">
            <div className="animate-spin inline-block w-8 h-8 border-[3px] border-current border-t-transparent text-indigo-600 rounded-full mb-3"></div>
            <p className="text-slate-500 italic text-sm">Đang tải danh sách yêu cầu duyệt...</p>
          </div>
        ) : requests.length === 0 ? (
          <div className="bg-white border border-slate-200 rounded-2xl p-16 text-center shadow-sm">
            <svg className="w-16 h-16 mx-auto text-slate-300 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
            </svg>
            <h3 className="text-lg font-bold text-slate-700 mb-1">Hiện không có yêu cầu nào</h3>
            <p className="text-slate-400 text-sm">Khi học sinh nộp dự án để duyệt, các yêu cầu sẽ xuất hiện ở đây.</p>
          </div>
        ) : (
          <div className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-slate-50 border-b border-slate-200 text-slate-500 text-xs font-bold uppercase tracking-wider">
                  <th className="p-4 pl-6">Dự án</th>
                  <th className="p-4">Học sinh</th>
                  <th className="p-4">Thời gian gửi</th>
                  <th className="p-4 text-center">Hành động</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-150">
                {requests.map((req) => (
                  <tr key={req.id} className="hover:bg-indigo-50/20 transition duration-150 text-sm">
                    <td className="p-4 pl-6">
                      <p className="font-bold text-slate-800">{req.projectTitle}</p>
                      <span className="text-[10px] text-slate-400 font-medium">ID dự án: {req.projectId}</span>
                    </td>
                    <td className="p-4">
                      <span className="font-semibold text-slate-700 bg-slate-100 px-2.5 py-1 rounded-lg border border-slate-200 text-xs">
                        Học sinh (ID: {req.studentId})
                      </span>
                    </td>
                    <td className="p-4 text-slate-500 text-xs">
                      {req.requestedAt ? new Date(req.requestedAt).toLocaleDateString('vi-VN') : 'Chưa rõ'}
                      <span className="block text-[10px] text-slate-400 mt-0.5">
                        {req.requestedAt ? new Date(req.requestedAt).toLocaleTimeString('vi-VN') : ''}
                      </span>
                    </td>
                    <td className="p-4">
                      <div className="flex justify-center">
                        <button 
                          onClick={() => handleOpenAudit(req)}
                          className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-bold rounded-lg transition shadow-sm hover:shadow-indigo-600/20"
                        >
                          Kiểm chứng & Đánh giá
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </main>

      {/* 4. MODAL CHI TIẾT KIỂM CHỨNG & REVIEW DỰ ÁN */}
      {selectedRequest && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 backdrop-blur-sm animate-in fade-in duration-200 p-4">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-4xl max-h-[90vh] overflow-hidden flex flex-col transform transition-all border border-slate-200">
            {/* Header Modal */}
            <div className="bg-indigo-900 text-white p-5 flex justify-between items-center shrink-0">
              <div>
                <h3 className="font-bold text-lg flex items-center gap-2">
                  <svg className="w-5 h-5 text-indigo-300" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" /></svg>
                  Đánh Giá & Đối Chiếu Dự Án
                </h3>
                <p className="text-xs text-indigo-200/80 mt-1">Dự án: {selectedRequest.projectTitle} | Yêu cầu ID: {selectedRequest.id}</p>
              </div>
              <button 
                onClick={() => { setSelectedRequest(null); setAuditDetails(null); }} 
                className="text-indigo-200 hover:text-white text-2xl font-bold bg-white/10 hover:bg-white/20 w-8 h-8 rounded-full flex items-center justify-center transition"
              >
                &times;
              </button>
            </div>

            {/* Body Modal */}
            <div className="flex-1 overflow-y-auto p-6 space-y-6 bg-slate-50/50 custom-scrollbar">
              {loadingAudit ? (
                <div className="flex flex-col items-center justify-center py-20 gap-3">
                  <div className="w-10 h-10 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin"></div>
                  <p className="text-slate-500 font-medium text-sm">Đang tải dữ liệu kiểm chứng và đối chiếu thực tế từ RAG...</p>
                </div>
              ) : auditDetails ? (
                <div className="space-y-6">
                  
                  {/* Báo cáo sơ lược */}
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm flex items-center justify-between">
                      <div>
                        <p className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">Tổng luận điểm</p>
                        <p className="text-xl font-bold text-slate-800 mt-1">{auditDetails.claims?.length || 0} Luận điểm</p>
                      </div>
                      <div className="w-10 h-10 rounded-lg bg-indigo-50 text-indigo-600 flex items-center justify-center font-bold">
                        C
                      </div>
                    </div>
                    <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm flex items-center justify-between">
                      <div>
                        <p className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">Tài liệu tham khảo đính kèm</p>
                        <p className="text-xl font-bold text-slate-800 mt-1">{auditDetails.sources?.length || 0} Nguồn tài liệu</p>
                      </div>
                      <div className="w-10 h-10 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center font-bold">
                        S
                      </div>
                    </div>
                  </div>

                  {/* Chi tiết từng Luận điểm */}
                  <div>
                    <h4 className="text-xs font-bold text-slate-400 uppercase tracking-widest mb-3">Mạng lưới đối chiếu luận điểm thực tế</h4>
                    <div className="space-y-3">
                      {(!auditDetails.claims || auditDetails.claims.length === 0) ? (
                        <div className="text-xs text-slate-400 italic text-center py-4 bg-white rounded-xl border border-slate-200">Dự án này chưa được thêm luận điểm nào.</div>
                      ) : (
                        auditDetails.claims.map((c, idx) => {
                          const graphInfo = c.graphData || {};
                          const hasEdge = graphInfo.status !== 'MISSING' && graphInfo.verdict;
                          return (
                            <div key={idx} className="bg-white border border-slate-200 rounded-xl p-4 shadow-sm relative overflow-hidden">
                              <div className="absolute left-0 top-0 bottom-0 w-1.5 bg-indigo-600"></div>
                              <div className="flex justify-between items-center mb-2 pl-2">
                                <span className="text-[9px] font-bold text-indigo-700 bg-indigo-50 border border-indigo-100 px-2 py-0.5 rounded">
                                  Luận điểm #{idx + 1}
                                </span>
                                {hasEdge ? (
                                  <span className={`text-[10px] font-black px-2 py-0.5 rounded border uppercase ${graphInfo.verdict === 'SUPPORTED' ? 'bg-emerald-50 text-emerald-700 border-emerald-200' : graphInfo.verdict === 'REFUTED' ? 'bg-rose-50 text-rose-700 border-rose-200' : 'bg-amber-50 text-amber-700 border-amber-200'}`}>
                                    {graphInfo.verdict}
                                  </span>
                                ) : (
                                  <span className="text-[10px] text-slate-400 bg-slate-100 px-2 py-0.5 rounded border border-slate-200 italic">Chưa chạy AI phân tích</span>
                                )}
                              </div>
                              
                              <p className="text-sm font-semibold text-slate-800 pl-2 leading-relaxed mb-3">"{c.content}"</p>
                              
                              {hasEdge && (
                                <div className="pl-2 border-l-2 border-dashed border-slate-200 mt-2 py-1 space-y-2 text-xs">
                                  <div className="flex justify-between">
                                    <span className="text-slate-400 font-medium">Độ tin cậy của AI:</span>
                                    <span className="font-bold text-slate-700">{(graphInfo.confidence * 100).toFixed(0)}%</span>
                                  </div>
                                  <div>
                                    <span className="block text-slate-500 font-bold mb-0.5">Lời giải thích đối chiếu từ AI:</span>
                                    <p className="italic text-slate-600 bg-slate-50 p-2.5 rounded-lg border border-slate-100 leading-relaxed">"{graphInfo.explanation}"</p>
                                  </div>
                                  
                                  {graphInfo.missing_evidence && graphInfo.missing_evidence.length > 0 && (
                                    <div>
                                      <span className="block text-rose-600 font-bold mb-0.5">Chứng cứ còn thiếu:</span>
                                      <ul className="list-disc pl-4 text-slate-600 space-y-0.5">
                                        {graphInfo.missing_evidence.map((me, i) => <li key={i}>{me}</li>)}
                                      </ul>
                                    </div>
                                  )}
                                </div>
                              )}
                            </div>
                          );
                        })
                      )}
                    </div>
                  </div>

                  {/* Chi tiết nguồn tài liệu đính kèm */}
                  <div>
                    <h4 className="text-xs font-bold text-slate-400 uppercase tracking-widest mb-3">Danh sách tài liệu chứng cứ (Sources)</h4>
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                      {(!auditDetails.sources || auditDetails.sources.length === 0) ? (
                        <div className="col-span-2 text-xs text-slate-400 italic text-center py-4 bg-white rounded-xl border border-slate-200">Không có tài liệu chứng cứ nào được đính kèm.</div>
                      ) : (
                        auditDetails.sources.map((s, idx) => (
                          <div key={idx} className="bg-white border border-slate-200 rounded-xl p-3 shadow-sm flex justify-between items-center text-xs">
                            <span className="font-medium text-slate-700 truncate max-w-[200px] flex items-center gap-1.5">
                              <svg className="w-4 h-4 text-indigo-500 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" /></svg>
                              {s.filename}
                            </span>
                            <span className="text-[10px] text-indigo-600 bg-indigo-50 font-bold px-2 py-0.5 rounded-full">{s.referenceCount} Trích dẫn</span>
                          </div>
                        ))
                      )}
                    </div>
                  </div>

                </div>
              ) : (
                <div className="text-center py-10 text-slate-400 italic text-sm">Không thể kết xuất dữ liệu sơ đồ đối chiếu.</div>
              )}

              {/* Phần viết Feedback */}
              <div className="bg-white border border-slate-200 rounded-xl p-4 shadow-sm shrink-0">
                <label className="block text-xs font-bold text-slate-400 uppercase tracking-widest mb-2">Nhận xét & Phản hồi (Feedback)</label>
                <textarea
                  placeholder="Nhập nội dung nhận xét chi tiết của bạn về dự án này..."
                  value={feedbackComment}
                  onChange={(e) => setFeedbackComment(e.target.value)}
                  className="w-full text-sm border border-slate-200 rounded-xl px-4 py-3 bg-slate-50/50 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 transition shadow-inner"
                  rows={4}
                  maxLength={10000}
                />
                <p className="text-[10px] text-slate-400 text-right mt-1">Tối đa 10,000 ký tự. Bắt buộc nhập nếu từ chối hoặc yêu cầu sửa đổi.</p>
              </div>
            </div>

            {/* Footer Modal với các nút hành động tương ứng API của BE */}
            <div className="p-4 border-t border-slate-100 bg-slate-50 flex flex-wrap gap-3 justify-end shrink-0">
              <button
                onClick={() => { setSelectedRequest(null); setAuditDetails(null); }}
                className="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-200 rounded-lg transition"
                disabled={submittingFeedback}
              >
                Hủy bỏ
              </button>
              
              <button
                onClick={() => handleReviewAction('RETURNED')}
                className="px-4 py-2 text-xs font-bold text-amber-700 bg-amber-100 hover:bg-amber-200 rounded-lg transition border border-amber-200"
                disabled={submittingFeedback}
              >
                {submittingFeedback ? 'Đang gửi...' : 'Yêu cầu sửa đổi (Return)'}
              </button>
              
              <button
                onClick={() => handleReviewAction('REJECTED')}
                className="px-4 py-2 text-xs font-bold text-rose-700 bg-rose-100 hover:bg-rose-200 rounded-lg transition border border-rose-200"
                disabled={submittingFeedback}
              >
                {submittingFeedback ? 'Đang gửi...' : 'Từ chối (Reject)'}
              </button>
              
              <button
                onClick={() => handleReviewAction('APPROVED')}
                className="px-4 py-2 text-xs font-bold text-white bg-indigo-600 hover:bg-indigo-700 rounded-lg transition shadow-sm hover:shadow-indigo-600/10"
                disabled={submittingFeedback}
              >
                {submittingFeedback ? 'Đang gửi...' : 'Phê duyệt (Approve)'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Toast Notification */}
      {toastMessage && (
        <div className="fixed bottom-5 right-5 z-50 bg-slate-900 text-white text-xs font-bold px-4 py-3 rounded-xl shadow-2xl transition duration-300 transform scale-100 animate-in fade-in slide-in-from-bottom-2">
          {toastMessage}
        </div>
      )}
    </div>
  );
}
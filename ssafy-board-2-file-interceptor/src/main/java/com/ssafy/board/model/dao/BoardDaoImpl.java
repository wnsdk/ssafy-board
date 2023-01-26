package com.ssafy.board.model.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.ssafy.board.model.BoardDto;
import com.ssafy.board.model.FileInfoDto;
import com.ssafy.util.DBUtil;

@Repository
public class BoardDaoImpl implements BoardDao {

	private final DataSource dataSource;
	private final DBUtil dbUtil;

	@Autowired
	public BoardDaoImpl(DataSource dataSource, DBUtil dbUtil) {
		this.dataSource = dataSource;
		this.dbUtil = dbUtil;
	}

	@Override
	public int writeArticle(BoardDto boardDto) throws SQLException {
		int cnt = 0;
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			StringBuilder sql = new StringBuilder();
			sql.append("insert into board (user_id, subject, content, hit, register_time) \n");
			sql.append("values (?, ?, ?, 0, now())");
			pstmt = conn.prepareStatement(sql.toString());
			pstmt.setString(1, boardDto.getUserId());
			pstmt.setString(2, boardDto.getSubject());
			pstmt.setString(3, boardDto.getContent());
			pstmt.executeUpdate();
			pstmt.close();

			List<FileInfoDto> fileInfos = boardDto.getFileInfos();
			if (fileInfos != null && !fileInfos.isEmpty()) {
				String lastNo = "select last_insert_id()";
				pstmt = conn.prepareStatement(lastNo);
				rs = pstmt.executeQuery();
				rs.next();
				int articleno = rs.getInt(1);
				pstmt.close();

				StringBuilder reigsterFile = new StringBuilder();
				reigsterFile.append("insert into file_info (article_no, save_folder, original_file, save_file) \n");
				reigsterFile.append("values");
				int size = fileInfos.size();
				for (int i = 0; i < size; i++) {
					reigsterFile.append("(?, ?, ?, ?)");
					if (i != fileInfos.size() - 1)
						reigsterFile.append(",");
				}
				pstmt = conn.prepareStatement(reigsterFile.toString());
				int idx = 0;
				for (int i = 0; i < size; i++) {
					FileInfoDto fileInfo = fileInfos.get(i);
					pstmt.setInt(++idx, articleno);
					pstmt.setString(++idx, fileInfo.getSaveFolder());
					pstmt.setString(++idx, fileInfo.getOriginalFile());
					pstmt.setString(++idx, fileInfo.getSaveFile());
				}
				pstmt.executeUpdate();
			}
			conn.commit();
			cnt = 1;
		} catch (SQLException e) {
			e.printStackTrace();
			conn.rollback();
			cnt = 0;
			throw new SQLException();
		} finally {
			dbUtil.close(rs, pstmt, conn);
		}
		return cnt;
	}

	@Override
	public List<BoardDto> listArticle(Map<String, String> map) throws SQLException {
		List<BoardDto> list = new ArrayList<BoardDto>();

		String key = map.get("key");
		String word = map.get("word");
		int start = Integer.parseInt(map.get("start"));
		int listsize = Integer.parseInt(map.get("listsize"));

		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = dataSource.getConnection();
			StringBuilder listArticle = new StringBuilder();
			listArticle.append(
					"select b.article_no, b.user_id, b.subject, b.content, b.hit, b.register_time, m.user_name \n");
			listArticle.append("from board b, members m \n");
			listArticle.append("where b.user_id = m.user_id \n");
			if (!word.isEmpty()) {
				if (key.equals("userid"))
					listArticle.append("and b.user_id = ? \n");
				else if (key.equals("subject"))
					listArticle.append("and b.subject like ? \n");
			}
			listArticle.append("order by b.article_no desc limit ?, ? \n");
			pstmt = conn.prepareStatement(listArticle.toString());
			int idx = 0;
			if (!word.isEmpty()) {
				if (key.equals("userid"))
					pstmt.setString(++idx, word);
				else if (key.equals("subject"))
					pstmt.setString(++idx, "%" + word + "%");
			}
			pstmt.setInt(++idx, start);
			pstmt.setInt(++idx, listsize);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				BoardDto boardDto = new BoardDto();
				boardDto.setArticleNo(rs.getInt("article_no"));
				boardDto.setUserId(rs.getString("user_id"));
				boardDto.setUserName(rs.getString("user_name"));
				boardDto.setSubject(rs.getString("subject").replace("<", "&lt;"));
				boardDto.setContent(rs.getString("content").replace("\n", "<br/>"));
				boardDto.setHit(rs.getInt("hit"));
				boardDto.setRegisterTime(rs.getString("register_time"));

				list.add(boardDto);
			}
		} finally {
			dbUtil.close(rs, pstmt, conn);
		}
		return list;
	}

	@Override
	public int getTotalArticleCount(Map<String, String> map) throws SQLException {
		int cnt = 0;

		String key = map.get("key");
		String word = map.get("word");

		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = dataSource.getConnection();
			StringBuilder sql = new StringBuilder();
			sql.append("select count(article_no) \n");
			sql.append("from board \n");
			if (!word.isEmpty()) {
				if (key.equals("userid"))
					sql.append("where user_id = ? \n");
				else if (key.equals("subject"))
					sql.append("where subject like ? \n");
			}
			pstmt = conn.prepareStatement(sql.toString());
			if (!word.isEmpty()) {
				if (key.equals("userid"))
					pstmt.setString(1, word);
				else if (key.equals("subject"))
					pstmt.setString(1, "%" + word + "%");
			}
			rs = pstmt.executeQuery();
			rs.next();
			cnt = rs.getInt(1);
		} finally {
			dbUtil.close(rs, pstmt, conn);
		}
		return cnt;
	}

	@Override
	public BoardDto getArticle(int articleNo) throws SQLException {
		BoardDto boardDto = null;
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = dataSource.getConnection();
			StringBuilder listArticle = new StringBuilder();
			listArticle.append(
					"select b.article_no, b.user_id, b.subject, b.content, b.hit, b.register_time, m.user_name \n");
			listArticle.append("from board b, members m \n");
			listArticle.append("where b.user_id = m.user_id \n");
			listArticle.append("and b.article_no = ? \n");
			pstmt = conn.prepareStatement(listArticle.toString());
			pstmt.setInt(1, articleNo);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				boardDto = new BoardDto();
				boardDto.setArticleNo(rs.getInt("article_no"));
				boardDto.setUserId(rs.getString("user_id"));
				boardDto.setUserName(rs.getString("user_name"));
				boardDto.setSubject(rs.getString("subject"));
				boardDto.setContent(rs.getString("content"));
				boardDto.setHit(rs.getInt("hit"));
				boardDto.setRegisterTime(rs.getString("register_time"));

				PreparedStatement pstmt2 = null;
				ResultSet rs2 = null;
				try {
					StringBuilder fileInfos = new StringBuilder();
					fileInfos.append("select save_folder, original_file, save_file \n");
					fileInfos.append("from file_info \n");
					fileInfos.append("where article_no = ?");
					pstmt2 = conn.prepareStatement(fileInfos.toString());
					pstmt2.setInt(1, articleNo);
					rs2 = pstmt2.executeQuery();
					List<FileInfoDto> files = new ArrayList<FileInfoDto>();
					while (rs2.next()) {
						FileInfoDto fileInfoDto = new FileInfoDto();
						fileInfoDto.setSaveFolder(rs2.getString("save_folder"));
						fileInfoDto.setOriginalFile(rs2.getString("original_file"));
						fileInfoDto.setSaveFile(rs2.getString("save_file"));

						files.add(fileInfoDto);
					}

					boardDto.setFileInfos(files);
				} finally {
					dbUtil.close(rs2, pstmt2);
				}
			}
		} finally {
			dbUtil.close(rs, pstmt, conn);
		}
		return boardDto;
	}

	@Override
	public void updateHit(int articleNo) throws SQLException {
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = dataSource.getConnection();
			StringBuilder sql = new StringBuilder();
			sql.append("update board \n");
			sql.append("set hit = hit + 1 \n");
			sql.append("where article_no = ?");
			pstmt = conn.prepareStatement(sql.toString());
			pstmt.setInt(1, articleNo);
			pstmt.executeUpdate();
		} finally {
			dbUtil.close(pstmt, conn);
		}
	}

	@Override
	public void modifyArticle(BoardDto boardDto) throws SQLException {
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = dataSource.getConnection();
			StringBuilder sql = new StringBuilder();
			sql.append("update board \n");
			sql.append("set subject = ?, content = ? \n");
			sql.append("where article_no = ?");
			pstmt = conn.prepareStatement(sql.toString());
			pstmt.setString(1, boardDto.getSubject());
			pstmt.setString(2, boardDto.getContent());
			pstmt.setInt(3, boardDto.getArticleNo());
			pstmt.executeUpdate();
		} finally {
			dbUtil.close(pstmt, conn);
		}
	}

	@Override
	public void deleteArticle(int articleNo) throws SQLException {
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);

			StringBuilder deleteFile = new StringBuilder();
			deleteFile.append("delete from file_info \n");
			deleteFile.append("where article_no = ?");
			pstmt = conn.prepareStatement(deleteFile.toString());
			pstmt.setInt(1, articleNo);
			pstmt.executeUpdate();
			pstmt.close();

			StringBuilder deleteArticle = new StringBuilder();
			deleteArticle.append("delete from board \n");
			deleteArticle.append("where article_no = ?");
			pstmt = conn.prepareStatement(deleteArticle.toString());
			pstmt.setInt(1, articleNo);
			pstmt.executeUpdate();
			conn.commit();
		} catch (SQLException e) {
			e.printStackTrace();
			conn.rollback();
			throw new SQLException();
		} finally {
			dbUtil.close(pstmt, conn);
		}
	}

}

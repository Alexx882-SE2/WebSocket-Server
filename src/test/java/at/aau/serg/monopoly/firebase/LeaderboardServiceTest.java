package at.aau.serg.monopoly.firebase;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaderboardServiceTest {

    private LeaderboardService leaderboardService;
    private Firestore firestore;

    private static MockedStatic<FirestoreClient> firestoreClientMock;

    @BeforeAll
    static void initStaticMock() {
        firestoreClientMock = Mockito.mockStatic(FirestoreClient.class);
    }

    @AfterAll
    static void closeStaticMock() {
        firestoreClientMock.close();
    }

    @BeforeEach
    void setup() {
        firestore = mock(Firestore.class);
        leaderboardService = new LeaderboardService();
        firestoreClientMock.when(FirestoreClient::getFirestore).thenReturn(firestore);
    }

    @Test
    void testUpdateUserStats_withValidGames() throws Exception {
        CollectionReference users = mock(CollectionReference.class);
        DocumentReference userDoc = mock(DocumentReference.class);
        CollectionReference history = mock(CollectionReference.class);
        ApiFuture<QuerySnapshot> future = mock(ApiFuture.class);
        QuerySnapshot snapshot = mock(QuerySnapshot.class);
        QueryDocumentSnapshot doc1 = mock(QueryDocumentSnapshot.class);
        QueryDocumentSnapshot doc2 = mock(QueryDocumentSnapshot.class);
        DocumentSnapshot userSnapshot = mock(DocumentSnapshot.class);
        ApiFuture<DocumentSnapshot> userFuture = mock(ApiFuture.class);

        when(firestore.collection("users")).thenReturn(users);
        when(users.document("uid")).thenReturn(userDoc);
        when(userDoc.collection("gameHistory")).thenReturn(history);
        when(history.get()).thenReturn(future);
        when(future.get()).thenReturn(snapshot);
        when(snapshot.getDocuments()).thenReturn(List.of(doc1, doc2));
        when(doc1.getData()).thenReturn(Map.of("won", true, "endMoney", 2000));
        when(doc2.getData()).thenReturn(Map.of("won", false, "endMoney", 1500));

        when(userDoc.get()).thenReturn(userFuture);
        when(userFuture.get()).thenReturn(userSnapshot);
        when(userSnapshot.exists()).thenReturn(true);
        when(userSnapshot.contains("name")).thenReturn(true);
        when(userSnapshot.getString("name")).thenReturn("TestUser");

        leaderboardService.updateUserStats("uid", firestore);

        verify(userDoc).set(argThat((Map<String, Object> map) ->
                map.get("wins").equals(1) &&
                        map.get("highestMoney").equals(2000) &&
                        map.get("averageMoney").equals(1750) &&
                        map.get("gamesPlayed").equals(2) &&
                        map.get("level").equals(1) &&
                        map.get("name").equals("TestUser")
        ), any(SetOptions.class));
    }

    @Test
    void testUpdateUserStatsEdgeCase_emptyHistory() throws Exception {
        CollectionReference users = mock(CollectionReference.class);
        DocumentReference userDoc = mock(DocumentReference.class);
        CollectionReference history = mock(CollectionReference.class);
        ApiFuture<QuerySnapshot> future = mock(ApiFuture.class);
        QuerySnapshot snapshot = mock(QuerySnapshot.class);

        when(firestore.collection("users")).thenReturn(users);
        when(users.document("uid")).thenReturn(userDoc);
        when(userDoc.collection("gameHistory")).thenReturn(history);
        when(history.get()).thenReturn(future);
        when(future.get()).thenReturn(snapshot);
        when(snapshot.getDocuments()).thenReturn(Collections.emptyList());

        leaderboardService.updateUserStats("uid", firestore);
        verify(userDoc, never()).set(anyMap(), any(SetOptions.class));
    }

    @Test
    void testUpdateLeaderboard_success() throws Exception {
        CollectionReference users = mock(CollectionReference.class);
        Query query = mock(Query.class);
        Query limitedQuery = mock(Query.class);
        ApiFuture<QuerySnapshot> future = mock(ApiFuture.class);
        QuerySnapshot snapshot = mock(QuerySnapshot.class);
        QueryDocumentSnapshot userDoc = mock(QueryDocumentSnapshot.class);
        mock(DocumentReference.class);

        Map<String, Object> userData = new HashMap<>();
        userData.put("wins", 3);
        userData.put("name", "Tester");

        when(firestore.collection("users")).thenReturn(users);
        when(users.orderBy(eq("wins"), any())).thenReturn(query);
        when(query.limit(50)).thenReturn(limitedQuery);
        when(limitedQuery.get()).thenReturn(future);
        when(future.get()).thenReturn(snapshot);
        when(snapshot.getDocuments()).thenReturn(List.of(userDoc));
        when(userDoc.getId()).thenReturn("123");
        when(userDoc.getData()).thenReturn(userData);

        CollectionReference lb = mock(CollectionReference.class);
        DocumentReference docRef = mock(DocumentReference.class);
        Query lbLimit = mock(Query.class);
        ApiFuture<QuerySnapshot> lbFuture = mock(ApiFuture.class);
        QuerySnapshot lbSnapshot = mock(QuerySnapshot.class);

        when(firestore.collection("leaderboard_wins")).thenReturn(lb);
        when(lb.limit(100)).thenReturn(lbLimit);
        when(lbLimit.get()).thenReturn(lbFuture);
        when(lbFuture.get()).thenReturn(lbSnapshot);
        when(lbSnapshot.getDocuments()).thenReturn(Collections.emptyList());

        when(lb.document(anyString())).thenReturn(docRef);

        leaderboardService.updateLeaderboard("wins", "leaderboard_wins");

        verify(docRef).set(argThat((Map<String, Object> m) ->
                m.get("name").equals("Tester") &&
                        m.get("userId").equals("123") &&
                        m.get("wins").equals(3) &&
                        m.get("rank").equals(1)
        ));
    }

    @Test
    void testDeleteCollection_recursive() throws Exception {
        CollectionReference collection = mock(CollectionReference.class);
        Query limitedQuery = mock(Query.class);
        ApiFuture<QuerySnapshot> future1 = mock(ApiFuture.class);
        ApiFuture<QuerySnapshot> future2 = mock(ApiFuture.class);
        QuerySnapshot snap1 = mock(QuerySnapshot.class);
        QuerySnapshot snap2 = mock(QuerySnapshot.class);
        QueryDocumentSnapshot doc = mock(QueryDocumentSnapshot.class);
        DocumentReference docRef = mock(DocumentReference.class);

        when(firestore.collection("some")).thenReturn(collection);
        when(collection.limit(100)).thenReturn(limitedQuery);
        when(limitedQuery.get()).thenReturn(future1, future2);
        when(future1.get()).thenReturn(snap1);
        when(future2.get()).thenReturn(snap2);
        when(doc.getReference()).thenReturn(docRef);
        when(snap1.getDocuments()).thenReturn(Collections.nCopies(100, doc));
        when(snap2.getDocuments()).thenReturn(Collections.emptyList());

        leaderboardService.deleteCollection(firestore, "some");

        verify(docRef, times(100)).delete();
    }
}

package searchengine.model;

import lombok.*;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "indexes_list")
public class IndexModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne()
    @JoinColumn(name = "page_id", nullable = false)
    private PageModel page;

    @ManyToOne()
    @JoinColumn(name = "lemma_id", nullable = false)
    private LemmaModel lemma;

    @Column(name = "index_rank",columnDefinition = "FLOAT", nullable = false)
    private float rank;
}
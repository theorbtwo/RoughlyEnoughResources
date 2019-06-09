import java.util.Map;

public class WorldGenDisplay implements RecipeDisplay<WorldGenRecipie> {
    private WorldGenRecipie recipie;
    private Block output_block;
    private Map<Integer, Double> prob_map;
    private int min_level = 9999;
    private int max_level = 0;

    public WorldGenDisplay(WorldGenRecipie recipie) {
	this.recipie = recipie;

	Map<Integer, Map<Block, Integer>> block_counts_at_level = null /* FIXME! */;
	Map<Integer, Integer> total_counts_at_level = null /* FIXME! */;

	for (int y : total_counts_at_level.keySet()) {
	    Map<Block, Integer> block_counts_at_this_level = block_counts_at_level.get(y);
	    double prob;
	    
	    if (block_counts_at_this_level.cotainsKey(output_block)) {
		prob = (double)block_counts_at_this_level.get(output_block) / total_counts_at_level.get(y);
	    } else {
		prob = 0;
	    }

	    prob_map.put(y, prob);

	    if (prob > 0) {
		if (y < min_level) {
		    min_level = y;
		}
		if (y > max_level) {
		    max_level = y;
		}
	    }
	}
    }
}
